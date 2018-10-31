package us.hebi.matlab.mat.format;

import us.hebi.matlab.common.util.Tasks;
import us.hebi.matlab.common.util.Tasks.IoTask;
import us.hebi.matlab.mat.types.*;
import us.hebi.matlab.mat.types.Cell;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Opaque;
import us.hebi.matlab.mat.types.Sparse;

import java.io.IOException;
import java.lang.Object;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.format.Mat5Type.Int32;
import static us.hebi.matlab.mat.format.Mat5Type.Int8;
import static us.hebi.matlab.mat.format.Mat5Type.UInt32;
import static us.hebi.matlab.mat.format.Mat5Type.UInt8;
import static us.hebi.matlab.mat.types.MatlabType.*;

/**
 * Reads MAT 5 files with the format as documented here:
 * @see <a href="http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf">MAT-File Format</a>
 *
 * @author Florian Enner
 * @since 30 Apr 2018
 */
public final class Mat5Reader {

    public static class ArrayHeader {

        public int getNumElements() {
            // [int32] as a single Mat5 element can't be larger
            return AbstractArray.getNumElements(dimensions);
        }

        public MatlabType getType() {
            return type;
        }

        public int[] getDimensions() {
            return dimensions;
        }

        public String getName() {
            return name;
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

        private ArrayHeader(int[] arrayFlags, MatlabType type, int[] dimensions, String name) {
            this.arrayFlags = arrayFlags;
            this.type = type;
            this.dimensions = dimensions;
            this.name = name;
        }

        final int[] arrayFlags;

        final MatlabType type;

        final int[] dimensions;

        final String name;

    }

    public interface ArrayFilter {
        boolean isAccepted(ArrayHeader header);
    }

    /**
     * Enables filtering of root-level entries based on the variable header
     *
     * @param filter root-level filter
     * @return this
     */
    public Mat5Reader setArrayFilter(ArrayFilter filter) {
        this.filter = checkNotNull(filter);
        return this;
    }

    /**
     * Inflating compressed entries is by far the most expensive part of reading a MAT5 file.
     * This method enables the inflation to happen concurrently, i.e., in multiple threads,
     * to speed up reading.
     * <p>
     * Note that this only works for sources that can read sub-sections (slices) of the data
     * such as buffers or memory mapped files. If the source does not support it, the source
     * will continue to be read using a single thread.
     *
     * @param executorService executorService
     * @return this
     */
    public Mat5Reader enableConcurrentDecompression(ExecutorService executorService) {
        this.executorService = checkNotNull(executorService);
        return this;
    }

    /**
     * Sets the buffer allocator that gets used for creating any buffer-backed array. Buffers
     * will be released when the array or containing mat file gets closed. This is not a
     * common use case, but may be useful when working with buffer pools or large memory
     * mapped buffers.
     *
     * @param bufferAllocator bufferAllocator
     * @return this
     */
    public Mat5Reader setBufferAllocator(BufferAllocator bufferAllocator) {
        this.bufferAllocator = bufferAllocator;
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

    /**
     * Disables processing of the (optional) subsystem that contains the data backing reference objects
     * such as handle classes (e.g. 'table'). The main reason for this method being public is that the
     * subsystem format is undocumented and unsupported. If you encounter a MAT file that fails processing,
     * disabling it will let you access at least all of the documented parts of the file. If this happens,
     * please also file a bug report.
     *
     * @return this
     */
    public Mat5Reader disableSubsystemProcessing() {
        this.processSubsystem = false;
        return this;
    }

    Mat5Reader setMcosRegistry(McosRegistry registry) {
        this.mcos = checkNotNull(registry);
        return this;
    }

    public final Mat5File readMat() throws IOException {
        try {

            // Read header and determine byte order
            long start = source.getPosition();
            Mat5File matFile = readMatHeader();
            this.subsysPosition = start + matFile.getSubsysOffset();

            // Generate content structure
            for (Future<NamedArray> task : readMatContent()) {
                NamedArray variable = task.get();
                if (variable != null) {
                    matFile.addArray(variable);
                }
            }

            // Process references
            if (matFile.getSubsystem() != null && processSubsystem)
                matFile.getSubsystem().processReferences(mcos);

            return matFile;

        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    private Mat5File readMatHeader() throws IOException {
        source.order(ByteOrder.nativeOrder());
        final Mat5File matFile;
        if (reducedHeader)
            matFile = Mat5File.readReducedFileHeader(source);
        else
            matFile = Mat5File.readFileHeader(source);
        source.order(matFile.getByteOrder());
        return matFile;
    }

    private List<Future<NamedArray>> readMatContent() throws IOException {
        List<Future<NamedArray>> content = new ArrayList<Future<NamedArray>>();
        Mat5Tag tag = Mat5Tag.readTagOrNull(source);
        while (tag != null) {
            content.add(readNamedRootArray(tag));
            tag = Mat5Tag.readTagOrNull(source);
        }
        return content;
    }

    private Mat5Tag readTag() throws IOException {
        return Mat5Tag.readTag(source);
    }

    private Future<NamedArray> readNamedRootArray(Mat5Tag tag) throws IOException {
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
                            return createChildReader(inflated)
                                    .atRoot(atSubsys)
                                    .readNamedArray();
                        } finally {
                            inflated.close();
                        }
                    }
                };

                // If possible execute it asynchronously
                boolean runAsync = !source.isMutatedByChildren() && executorService != null;
                return runAsync ? executorService.submit(task) : Tasks.wrapAsFuture(task.call());

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
    private Mat5Reader atRoot(boolean atSubsys) {
        nextIsSubsys = atSubsys;
        mayFilterNext = true;
        return this;
    }

    private boolean isAccepted(ArrayHeader header) {
        try {
            if (filter == null || !mayFilterNext || nextIsSubsys)
                return true;
            return filter.isAccepted(header);
        } finally {
            mayFilterNext = false;
        }
    }

    private NamedArray readNamedArray() throws IOException {
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
        return new Mat5Subsystem(header.getDimensions(), header.isGlobal(), buffer, bufferAllocator);
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

    private static String readAsAscii(Mat5Tag tag) throws IOException {
        return CharEncoding.parseAsciiString(tag.readAsBytes());
    }

    private NumberStore readAsNumberStore(Mat5Tag tag) throws IOException {
        return new UniversalNumberStore(tag.getType(), readAsByteBuffer(tag), bufferAllocator);
    }

    private ByteBuffer readAsByteBuffer(Mat5Tag tag) throws IOException {
        ByteBuffer buffer = bufferAllocator.allocate(tag.getNumBytes());
        buffer.order(source.order());
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

    static IOException readError(String format, Object... args) {
        return new IOException(String.format(format, args));
    }

    // ------------------------- Factory methods

    private Mat5Reader createChildReader(Source source) {
        Mat5Reader reader = new Mat5Reader(source);
        reader.filter = this.filter;
        reader.mcos = this.mcos;
        reader.bufferAllocator = this.bufferAllocator;
        return reader;
    }

    private Matrix createMatrix(int[] dimensions, MatlabType type, boolean global, boolean logical, NumberStore real, NumberStore imaginary) {
        return new MatMatrix(dimensions, global, type, logical, real, imaginary);
    }

    private Sparse createSparse(int[] dimensions, boolean global, boolean logical, int nzMax, NumberStore real, NumberStore imaginary, NumberStore rowIndices, NumberStore colIndices) {
        return new MatSparseCSC(dimensions, global, logical, nzMax, real, imaginary, rowIndices, colIndices);
    }

    private Char createChar(int[] dims, boolean global, CharEncoding encoding, CharBuffer buffer) {
        return new MatChar(dims, global, encoding, buffer);
    }

    private Cell createCell(int[] dims, boolean global, Array[] contents) {
        return new MatCell(dims, global, contents);
    }

    private Struct createStruct(int[] dims, boolean isGlobal, String[] names, Array[][] values) {
        return new MatStruct(dims, isGlobal, names, values);
    }

    private ObjectStruct createObject(int[] dims, boolean isGlobal, String className, String[] names, Array[][] values) {
        return new MatObjectStruct(dims, isGlobal, className, names, values);
    }

    private Opaque createOpaque(boolean isGlobal, String objectType, String className, Array content) {
        // Serialized Java object
        if ("java".equals(objectType))
            return new MatJavaObject(isGlobal, className, content);

        // MCOS (Matlab Class Object System) related entries
        if ("MCOS".equals(objectType)) {
            if ("FileWrapper__".equals(className)) {
                // Special element that contains data for reference objects
                return new McosFileWrapper(isGlobal, objectType, className, content, source.order());
            } else {
                // Pointer to a reference object
                return mcos.register(McosReference.parseOpaque(isGlobal, objectType, className, content));
            }
        }

        // Generic Opaque object
        return new MatOpaque(isGlobal, objectType, className, content);
    }

    Mat5Reader(Source source) {
        this.source = checkNotNull(source, "Source can't be empty");
    }

    private final Source source;

    private int numEntries = 0;
    private long subsysPosition = Long.MIN_VALUE;
    private boolean nextIsSubsys = false;
    private boolean mayFilterNext = false;
    private boolean reducedHeader = false;
    private ArrayFilter filter = null;
    private ExecutorService executorService = null;
    private boolean processSubsystem = true;
    private int maxInflateBufferSize = 2048;
    private McosRegistry mcos = new McosRegistry();
    private BufferAllocator bufferAllocator = Mat5.getDefaultBufferAllocator();

}
