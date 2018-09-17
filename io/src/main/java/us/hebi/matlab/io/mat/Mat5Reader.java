package us.hebi.matlab.io.mat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import us.hebi.matlab.common.util.Silencer;
import us.hebi.matlab.common.util.Tasks;
import us.hebi.matlab.common.util.Tasks.IoTask;
import us.hebi.matlab.io.types.*;
import us.hebi.matlab.io.types.Cell;
import us.hebi.matlab.io.types.Matrix;
import us.hebi.matlab.io.types.Opaque;
import us.hebi.matlab.io.types.Sparse;

import java.io.Closeable;
import java.io.IOException;
import java.lang.Object;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.io.mat.Mat5Type.*;
import static us.hebi.matlab.io.mat.Mat5Type.Int32;
import static us.hebi.matlab.io.mat.Mat5Type.Int8;
import static us.hebi.matlab.io.mat.Mat5Type.UInt32;
import static us.hebi.matlab.io.mat.Mat5Type.UInt8;
import static us.hebi.matlab.io.types.MatlabType.*;

/**
 * Reads MAT 5 files with the format as documented here:
 * {@see http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf}
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 30 Apr 2018
 */
public class Mat5Reader implements Closeable {

    protected Mat5Reader(Source source) {
        this.source = checkNotNull(source);
    }

    public Mat5Reader setFilter(ArrayFilter filter) {
        this.filter = checkNotNull(filter);
        return this;
    }

    public Mat5Reader enableAsyncDecompression(ExecutorService executorService) {
        return enableAsyncDecompression(executorService, true);
    }

    public Mat5Reader enableAsyncDecompression(ExecutorService executorService, boolean checkSourceSupport) {
        if (checkSourceSupport && source.isMutatedByChildren())
            throw new IllegalArgumentException("Async decompression is not supported by the specified source");
        this.asyncExecutor = checkNotNull(executorService);
        return this;
    }

    public Mat5Reader setReducedHeader(boolean reducedHeader) {
        this.reducedHeader = reducedHeader;
        return this;
    }

    public Mat5Reader setMaxInflateBufferSize(int maxInflateBufferSize) {
        this.maxInflateBufferSize = maxInflateBufferSize;
        return this;
    }

    public Mat5Reader setMcosRegistry(McosRegistry registry) {
        this.mcos = checkNotNull(registry);
        return this;
    }

    public final Mat5File readFile() throws IOException {
        return readFile(true);
    }

    public final Mat5File readFile(boolean processReferences) throws IOException {
        try {

            // Read header and determine byte order
            long start = source.getPosition();
            Mat5File matFile = readFileWithoutContent();
            this.subsysPosition = start + matFile.getSubsysOffset();

            // Generate content structure
            for (Future<NamedArray> task : readContent()) {
                NamedArray variable = task.get();
                if (variable != null) {
                    matFile.addArray(variable);
                }
            }

            // Process references
            if (matFile.getSubsystem() != null && processReferences)
                matFile.getSubsystem().processReferences(mcos);

            return matFile;

        } catch (ExecutionException exe) {
            throw new IOException(exe);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

    }

    public final Mat5File readFileWithoutContent() throws IOException {
        source.setByteOrder(ByteOrder.nativeOrder());
        final Mat5File matFile;
        if (reducedHeader)
            matFile = Mat5File.readReducedFileHeader(source);
        else
            matFile = Mat5File.readFileHeader(source);
        source.setByteOrder(matFile.getByteOrder());
        return matFile;
    }

    public final List<Future<NamedArray>> readContent() throws IOException {
        List<Future<NamedArray>> content = new ArrayList<Future<NamedArray>>();
        Mat5Tag tag = Mat5Tag.readTagOrNull(source);
        while (tag != null) {
            content.add(readNamedRootArray(tag));
            tag = Mat5Tag.readTagOrNull(source);
        }
        return content;
    }

    protected final Mat5Tag readTag() throws IOException {
        return Mat5Tag.readTag(source);
    }

    public final Future<NamedArray> readNamedRootArray(Mat5Tag tag) throws IOException {
        checkArgument(tag.getNumBytes() != 0, "Root element contains no data");
        long expectedEnd = source.getPosition() + tag.getNumBytes() + tag.getPadding();


        final boolean atSubsys;
        if (!reducedHeader) {
            // Normal file: offset is inside header
            atSubsys = (subsysPosition == source.getPosition() - Mat5.MATRIX_TAG_SIZE);
        } else {
            // Reduced: subsystem is always the second entry
            numEntries++;
            atSubsys = (numEntries == 2);
        }

        try {

            // Root element (e.g. array/cell array/struct etc.) is stored uncompressed.
            // Since we don't have an independent view on the data, we can't defer parsing
            // and do it immediately in the main thread.
            if (tag.getType() == Matrix) {
                return Tasks.wrapAsFuture(atRoot(atSubsys).readNamedArrayWithoutTag(tag));
            }

            // Root element is stored compressed using the 'deflate' algorithm. Depending on
            // the source, we may be able to do the decompression in a background thread. The
            // decompression is the most CPU intensive part, so this can result in significant
            // gains, especially on large files. Note that only root elements can be compressed.
            if (tag.getType() == Compressed) {

                // Create an independent Source for the decompressed data
                int bufferSize = Math.min(tag.getNumBytes() * 2, maxInflateBufferSize);
                final Source inflated = source.readInflated(tag.getNumBytes(), bufferSize);

                // Read array in a task
                IoTask<NamedArray> task = new IoTask<NamedArray>() {
                    @Override
                    public NamedArray call() throws IOException {
                        try {
                            return createReader(inflated)
                                    .setMcosRegistry(mcos)
                                    .atRoot(atSubsys)
                                    .readNamedArray();
                        } finally {
                            Silencer.close(inflated);
                        }
                    }
                };

                // If possible execute it asynchronously
                boolean runAsync = !source.isMutatedByChildren() && asyncExecutor != null;
                return runAsync ? asyncExecutor.submit(task) : Tasks.wrapAsFuture(task.call());

            }

            throw readError("Expected 'Compressed' or 'Matrix' tag. Found: %s", tag.getType());

        } finally {
            // Move ahead to the next entry in case one was skipped. We do this here so that
            // we can make sure that we're always skipping in the root source rather than
            // doing a potentially expensive skip inside a compressed source.
            long remaining = expectedEnd - source.getPosition();
            if (remaining > 0)
                source.skip(remaining);
        }
    }

    /**
     * Indicates that the parser is at the root level and that
     * the next matrix is a root element. This may enable
     * additional options such as filtering.
     *
     * @return this
     */
    protected Mat5Reader atRoot(boolean atSubsys) {
        nextIsSubsys = atSubsys;
        mayFilterNext = true;
        return this;
    }

    private boolean isAccepted(ArrayHeader header) {
        try {
            return !mayFilterNext || filter == null || nextIsSubsys || filter.isAccepted(header);
        } finally {
            mayFilterNext = false;
        }
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public final NamedArray readNamedArray() throws IOException {
        Mat5Tag tag = readTagWithExpectedType(Matrix);
        // Sometimes there are completely empty Matrix tags. In that
        // case, return empty matrix rather than null.
        if (tag.getNumBytes() == 0)
            return new NamedArray("", Mat5.EMPTY_MATRIX);
        return readNamedArrayWithoutTag(tag);
    }

    private NamedArray readNamedArrayWithoutTag(Mat5Tag tag) throws IOException {
        long start = source.getPosition();
        NamedArray value = readNamedArrayWithoutTag();
        long numBytes = source.getPosition() - start;

        // Sanity check that data was read fully or skipped.
        // Note that we don't skip to the end as we may be
        // reading from a deflated source that is expensive
        // to skip.
        if (tag.getNumBytes() == numBytes || value == null)
            return value;

        throw readError("Specified matrix tag does not match content size. Tag: %d, Content: %d", tag.getNumBytes(), numBytes);
    }

    private NamedArray readNamedArrayWithoutTag() throws IOException {
        // Subfield 1: meta data
        int[] arrayFlags = readTagWithExpectedType(UInt32).readAsInts();
        if (arrayFlags.length != 2)
            throw readError("Unexpected size of array flags. Expected %d, Found %d", 2, arrayFlags.length);
        MatlabType type = Mat5ArrayFlags.getType(arrayFlags);

        // Opaque types have a different format
        if (type == Opaque) {
            mayFilterNext = false;
            return readOpaque(arrayFlags);
        }

        // Subfield 2: Dimensions
        final int[] dimensions = readTagWithExpectedType(Int32).readAsInts();
        if (dimensions.length < 2) {
            throw readError("Expected at least 2 dimensions. Found %d", dimensions.length);
        }

        // Subfield 3: Name
        String name = readAsAscii(readTagWithExpectedType(Int8));
        ArrayHeader header = new ArrayHeader(arrayFlags, type, dimensions, name);

        // Check if we should continue to read the content
        if (!isAccepted(header))
            return null;

        // Subsystem, e.g. class object information
        if (nextIsSubsys) {
            try {
                return new NamedArray(name, readSubsystem(header));
            } finally {
                nextIsSubsys = false;
            }
        }

        // Subsequent fields are specific to each type
        final Array array;
        switch (header.getType()) {
            case Double:
            case Single:
            case Int8:
            case UInt8:
            case Int16:
            case UInt16:
            case Int32:
            case UInt32:
            case Int64:
            case UInt64:
                array = readNumerical(header);
                break;

            case Sparse:
                array = readSparse(header);
                break;

            case Character:
                array = readChar(header);
                break;

            case Cell:
                array = readCell(header);
                break;

            case Structure:
                array = readStruct(header);
                break;

            case Object:
                array = readObject(header);
                break;

            case Function:
                array = readFunctionHandle(header);
                break;

            case Opaque:
                throw new AssertionError("Should not get here");

            default:
                throw readError("Found unsupported type: %s", type);
        }

        return new NamedArray(name, array);
    }

    private Array readSubsystem(ArrayHeader header) throws IOException {
        if (header.isComplex())
            throw readError("Subsystem can't be complex");
        if (header.getType() != MatlabType.UInt8)
            throw readError("Unexpected Subsystem class type. Expected: %s, Found %s", UInt8, header.getType());

        // Store data as bytes directly
        ByteBuffer buffer = readAsByteBuffer(readTagWithExpectedType(UInt8));
        return new Mat5Subsystem(header.getDimensions(), header.isGlobal(), buffer);
    }

    private Array readNumerical(ArrayHeader header) throws IOException {
        // Subfield 4: Real part (pr)
        NumberStore real = readAsNumberStore(readTag());

        // Subfield 5: Imaginary part (pi) (optional)
        NumberStore imaginary = null;
        if (header.isComplex()) {
            imaginary = readAsNumberStore(readTag());
        }
        return createMatrix(header.getDimensions(), header.getType(),
                header.isGlobal(), header.isLogical(),
                real, imaginary);

    }

    private Array readSparse(ArrayHeader header) throws IOException {

        // Subfield 4: Row Index (ir)
        NumberStore rowIndices = readAsNumberStore(readTagWithExpectedType(Int32));

        // Subfield 5: Column Index (jc)
        NumberStore colIndices = readAsNumberStore(readTagWithExpectedType(Int32));

        // Subfield 6: Real part (pr)
        NumberStore real = readAsNumberStore(readTag());

        // Subfield 7: Imaginary part (pi)
        NumberStore imaginary = null;
        if (header.isComplex()) {
            imaginary = readAsNumberStore(readTag());
        }

        return createSparse(header.getDimensions(), header.isGlobal(), header.isLogical(),
                header.getNzMax(), real, imaginary, rowIndices, colIndices);
    }

    private Array readChar(ArrayHeader header) throws IOException {

        // Subfield 4: Data
        Mat5Tag tag = readTag();
        CharEncoding encoding = tag.getType().getCharEncoding();
        CharBuffer buffer = encoding.readCharBuffer(source, tag.getNumBytes());
        source.skip(tag.getPadding());
        return createChar(header.getDimensions(), header.isGlobal(), encoding, buffer);

    }

    private Array readCell(ArrayHeader header) throws IOException {

        // Subfield 4: Array of Cells Subelements. Stored in column major order
        final Array[] contents = new Array[header.getNumElements()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = readNamedArray().getValue();
        }

        return createCell(header.getDimensions(), header.isGlobal(), contents);
    }

    private Array readStruct(ArrayHeader header) throws IOException {
        // Struct has the same structure as an object without a name
        return readStructOrObject(header, null);
    }

    private Array readObject(ArrayHeader header) throws IOException {
        // Subfield 4: Class name
        String className = readAsAscii(readTagWithExpectedType(Int8));
        return readStructOrObject(header, className);
    }

    private Array readStructOrObject(ArrayHeader header, String objectClassName) throws IOException {
        // Subfield 4/5: Field Name Length
        int[] result = readTagWithExpectedType(Int32).readAsInts();
        checkArgument(result.length == 1, "Incorrect number of values for max field name length");
        final int maxLength = result[0];

        // Subfield 5/6: Field Names
        // Note that this contains series of strings that each 'max length' of
        // space and are ended by the null character
        byte[] buffer = readTagWithExpectedType(Int8).readAsBytes();
        final int numFields = (maxLength == 0) ? 0 : buffer.length / maxLength;
        final String[] names = new String[numFields];
        for (int i = 0; i < numFields; i++) {
            names[i] = CharEncoding.parseAsciiString(buffer, i * maxLength, maxLength);
        }

        // Subfield 6/7: Fields ([f f f f ...] * cols * rows)
        int numElements = header.getNumElements();
        final Array[][] values = new Array[numFields][numElements];
        for (int i = 0; i < numElements; i++) {
            for (int field = 0; field < numFields; field++) {
                values[field][i] = readNamedArray().getValue();
            }
        }

        if (objectClassName == null)
            return createStruct(header.getDimensions(), header.isGlobal(), names, values);
        return createObject(header.getDimensions(), header.isGlobal(), objectClassName, names, values);
    }

    private Array readFunctionHandle(ArrayHeader header) throws IOException {
        Struct content = (Struct) readNamedArray().getValue();
        return new MatFunction(header.isGlobal(), content);
    }

    /**
     * Opaque types are used to store various non-covered types such as function handles,
     * tables, and strings. They use a slightly different structure than other types, so
     * we can't share the same header (e.g. there is no dimension array).
     * <p>
     * Note that this is not in the official documentation. The implementation is based
     * on MatFileRW's MatFileReader and personal tests.
     */
    private NamedArray readOpaque(int[] arrayFlags) throws IOException {
        boolean isGlobal = Mat5ArrayFlags.isGlobal(arrayFlags);

        // Subfield 2: Ascii variable name
        String name = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 3: Used to store the object type identifier, e.g., "MCOS" or "handle"
        String objectType = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 4: Class Name, e.g., "table" or "string"
        String className = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 5: Content
        Array content = readNamedArray().getValue();

        return new NamedArray(name, createOpaque(isGlobal, objectType, className, content));

    }

    public interface ArrayFilter {
        boolean isAccepted(ArrayHeader header);
    }

    @RequiredArgsConstructor
    public static class ArrayHeader {

        public int getNumElements() {
            // [int32] as a single Mat5 element can't be larger
            return AbstractArray.getNumElements(dimensions);
        }

        public boolean isLogical() {
            return Mat5ArrayFlags.isLogical(arrayFlags);
        }

        public boolean isComplex() {
            return Mat5ArrayFlags.isComplex(arrayFlags);
        }

        public boolean isGlobal() {
            return Mat5ArrayFlags.isGlobal(arrayFlags);
        }

        public int getNzMax() {
            return Mat5ArrayFlags.getNzMax(arrayFlags);
        }

        final int[] arrayFlags;

        @Getter
        final MatlabType type;

        @Getter
        final int[] dimensions;

        @Getter
        final String name;

    }

    private static String readAsAscii(Mat5Tag tag) throws IOException {
        return CharEncoding.parseAsciiString(tag.readAsBytes());
    }

    private NumberStore readAsNumberStore(Mat5Tag tag) throws IOException {
        return new UniversalNumberStore(tag.getType(), readAsByteBuffer(tag));
    }

    private ByteBuffer readAsByteBuffer(Mat5Tag tag) throws IOException {
        ByteBuffer buffer = Mat5.allocateBuffer(tag.getNumBytes());
        buffer.order(source.getByteOrder());
        source.readByteBuffer(buffer);
        source.skip(tag.getPadding());
        buffer.rewind();
        return buffer;
    }

    private Mat5Tag readTagWithExpectedType(Mat5Type expected) throws IOException {
        Mat5Tag tag = readTag();
        if (tag.getType() != expected)
            throw readError("Encountered unexpected tag. Expected %s, Found %s", expected, tag.getType());
        return tag;
    }

    protected static final IOException readError(String format, Object... args) {
        return new IOException(String.format(format, args));
    }

    // ------------------------- Overridable factory methods

    protected Mat5Reader createReader(Source source) {
        return new Mat5Reader(source);
    }

    protected Matrix createMatrix(int[] dimensions, MatlabType type, boolean global, boolean logical, NumberStore real, NumberStore imaginary) {
        return new MatMatrix(dimensions, global, type, logical, real, imaginary);
    }

    protected Sparse createSparse(int[] dimensions, boolean global, boolean logical, int nzMax, NumberStore real, NumberStore imaginary, NumberStore rowIndices, NumberStore colIndices) {
        return new MatSparseCSC(dimensions, global, logical, nzMax, real, imaginary, rowIndices, colIndices);
    }

    protected Char createChar(int[] dims, boolean global, CharEncoding encoding, CharBuffer buffer) {
        return new MatChar(dims, global, encoding, buffer);
    }

    protected Cell createCell(int[] dims, boolean global, Array[] contents) {
        return new MatCell(dims, global, contents);
    }

    protected Struct createStruct(int[] dims, boolean isGlobal, String[] names, Array[][] values) {
        return new MatStruct(dims, isGlobal, names, values);
    }

    protected ObjectStruct createObject(int[] dims, boolean isGlobal, String className, String[] names, Array[][] values) {
        return new MatObjectStruct(dims, isGlobal, className, names, values);
    }

    protected Opaque createOpaque(boolean isGlobal, String objectType, String className, Array content) {
        // Serialized Java object
        if ("java".equals(objectType))
            return new MatJavaObject(isGlobal, className, content);

        // MCOS (Matlab Class Object System) related entries
        if ("MCOS".equals(objectType)) {
            if ("FileWrapper__".equals(className)) {
                return new McosFileWrapper(isGlobal, objectType, className, content, source.getByteOrder());
            } else {
                return mcos.register(McosReference.parseOpaque(isGlobal, objectType, className, content));
            }
        }

        // Generic Opaque object
        return new MatOpaque(isGlobal, objectType, className, content);
    }

    private final Source source;

    private int numEntries = 0;
    private long subsysPosition = Long.MIN_VALUE;
    private boolean nextIsSubsys = false;
    private boolean mayFilterNext = false;
    private boolean reducedHeader = false;
    private ArrayFilter filter = null;
    private ExecutorService asyncExecutor = null;
    private int maxInflateBufferSize = 2048;
    private McosRegistry mcos = new McosRegistry();

}
