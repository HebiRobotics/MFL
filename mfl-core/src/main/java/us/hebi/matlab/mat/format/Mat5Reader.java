/*-
 * #%L
 * MAT File Library
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.format.CharEncoding.CloseableCharBuffer;
import us.hebi.matlab.mat.types.Cell;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Opaque;
import us.hebi.matlab.mat.types.Sparse;
import us.hebi.matlab.mat.types.*;
import us.hebi.matlab.mat.util.Casts;
import us.hebi.matlab.mat.util.Tasks;
import us.hebi.matlab.mat.util.Tasks.IoTask;

import java.io.IOException;
import java.lang.Object;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static us.hebi.matlab.mat.format.Mat5Type.Int32;
import static us.hebi.matlab.mat.format.Mat5Type.Int8;
import static us.hebi.matlab.mat.format.Mat5Type.UInt32;
import static us.hebi.matlab.mat.format.Mat5Type.UInt8;
import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.types.MatlabType.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Reads MAT 5 files with the format as documented here:
 *
 * @author Florian Enner
 * @see <a href="http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf">MAT-File Format</a>
 * @since 30 Apr 2018
 */
public class Mat5Reader {

    public static class EntryHeader {

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

        private EntryHeader(int[] arrayFlags, MatlabType type, int[] dimensions, String name) {
            this.arrayFlags = arrayFlags;
            this.type = type;
            this.dimensions = dimensions;
            this.name = name;
        }

        @Override
        public String toString() {
            return "EntryHeader{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", dimensions=" + Arrays.toString(dimensions) +
                    (isGlobal() ? ", global" : "") +
                    (isLogical() ? ", logical" : "") +
                    (isComplex() ? ", complex" : "") +
                    '}';
        }

        final int[] arrayFlags;

        final MatlabType type;

        final int[] dimensions;

        final String name;

    }

    public interface EntryFilter {
        boolean isAccepted(EntryHeader header);
    }

    /**
     * Enables filtering of root-level entries based on the entry header
     *
     * @param filter root-level filter
     * @return this
     */
    public Mat5Reader setEntryFilter(EntryFilter filter) {
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
            for (Future<MatFile.Entry> task : readMatContent()) {
                MatFile.Entry entry = task.get();
                if (entry != null) {
                    matFile.addEntry(entry);
                }
            }

            // Process references
            if (matFile.getSubsystem() != null && processSubsystem)
                ((Mat5Subsystem) matFile.getSubsystem().getValue()).processReferences(mcos);

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

    private List<Future<MatFile.Entry>> readMatContent() throws IOException {
        List<Future<MatFile.Entry>> content = new ArrayList<Future<MatFile.Entry>>();
        Mat5Tag tag = Mat5Tag.readTagOrNull(source);
        while (tag != null) {
            content.add(readEntry(tag));
            tag = Mat5Tag.readTagOrNull(source);
        }
        return content;
    }

    private Mat5Tag readTag() throws IOException {
        return Mat5Tag.readTag(source);
    }

    private Future<MatFile.Entry> readEntry(Mat5Tag tag) throws IOException {
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
                return Tasks.wrapAsFuture(atRoot(atSubsys).readEntryWithoutTag(tag));
            }

            // Root element is stored compressed using the 'deflate' algorithm. Depending on
            // the source, we may be able to do the decompression in a background thread. The
            // decompression is the most CPU intensive part, so this can result in significant
            // gains, especially on large files. Note that only root elements can be compressed.
            if (tag.getType() == Compressed) {

                // Create an independent Source for the decompressed data
                int bufferSize = tag.getNumBytes() * 2;
                if (bufferSize > maxInflateBufferSize || bufferSize < 0 /* overflow >1 GB */) {
                    bufferSize = maxInflateBufferSize;
                }
                final Source inflated = source.readInflated(tag.getNumBytes(), bufferSize);

                // Read array in a task
                IoTask<MatFile.Entry> task = new IoTask<MatFile.Entry>() {
                    @Override
                    public MatFile.Entry call() throws IOException {
                        try {
                            return createChildReader(inflated)
                                    .atRoot(atSubsys)
                                    .readEntry();
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

    private boolean isAccepted(EntryHeader header) {
        try {
            if (filter == null || !mayFilterNext || nextIsSubsys)
                return true;
            return filter.isAccepted(header);
        } finally {
            mayFilterNext = false;
        }
    }

    protected Array readNestedArray() throws IOException {
        return readEntry().getValue();
    }

    private MatFile.Entry readEntry() throws IOException {
        Mat5Tag tag = readTagWithExpectedType(Matrix);
        // Sometimes there are completely empty Matrix tags. In that
        // case, return empty matrix rather than null.
        if (tag.getNumBytes() == 0)
            return new MatFile.Entry("", false, Mat5.EMPTY_MATRIX);
        return readEntryWithoutTag(tag);
    }

    private MatFile.Entry readEntryWithoutTag(Mat5Tag tag) throws IOException {
        // MATLAB defines a limit of <2GB per entry, but Octave apparently treats
        // the size as a uint32 and allows <4GB entries. Interestingly, MATLAB 2021a
        // is able to load these entries, but is unable to re-save them.
        //
        // This case was encountered in issue #64 where a root level struct contains
        // multiple <2GB variables and reaches a total >2GB size.
        //
        // In order to match MATLAB behavior, we allow >2GB entries here and fail at a
        // later stage if we encounter a numerical matrix that is out of the Java
        // limits. Note that we do not have to worry about computing padding of a negative
        // number because entries are already aligned and always have zero padding.
        final long expectedBytes = Casts.uint32(tag.getNumBytes());
        if (expectedBytes > Integer.MAX_VALUE) {
            String warning = String.format("[MFL] encountered illegal entry larger than 2GB: %.1fGB.",
                    expectedBytes / 1024d / 1024d / 1024d);
            System.err.println(warning);
        }

        long start = source.getPosition();
        MatFile.Entry value = readEntryWithoutTag();
        long numBytes = source.getPosition() - start;

        // Sanity check that data was read fully or skipped.
        // Note that we don't skip to the end as we may be
        // reading from a deflated source that is expensive
        // to skip.
        if (expectedBytes == numBytes || value == null)
            return value;

        throw readError("Specified matrix tag does not match content size. Tag: %d, Content: %d", tag.getNumBytes(), numBytes);
    }

    private MatFile.Entry readEntryWithoutTag() throws IOException {
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
        EntryHeader header = new EntryHeader(arrayFlags, type, dimensions, name);

        // Check if we should continue to read the content
        if (!isAccepted(header))
            return null;

        // Subsystem, e.g. class object information
        if (nextIsSubsys) {
            try {
                return new MatFile.Entry(name, header.isGlobal(), readSubsystem(header));
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

        return new MatFile.Entry(name, header.isGlobal(), array);
    }

    private Array readSubsystem(EntryHeader header) throws IOException {
        if (header.isComplex())
            throw readError("Subsystem can't be complex");
        if (header.getType() != MatlabType.UInt8)
            throw readError("Unexpected Subsystem class type. Expected: %s, Found %s", UInt8, header.getType());

        // Store data as bytes directly
        ByteBuffer buffer = readAsByteBuffer(readTagWithExpectedType(UInt8));
        return new Mat5Subsystem(header.getDimensions(), buffer, bufferAllocator);
    }

    private Array readNumerical(EntryHeader header) throws IOException {
        // Subfield 4: Real part (pr)
        NumberStore real = readAsNumberStore(readTag());

        // Subfield 5: Imaginary part (pi) (optional)
        NumberStore imaginary = null;
        if (header.isComplex()) {
            imaginary = readAsNumberStore(readTag());
        }
        return createMatrix(header.getDimensions(), header.getType(), header.isLogical(),
                real, imaginary);

    }

    private Array readSparse(EntryHeader header) throws IOException {

        // Subfield 4: Row Index (ir)
        NumberStore rowIndices = readAsNumberStore(readTagWithExpectedType(Int32));
        if (header.getNzMax() == 1 && rowIndices.getNumElements() == 0) {
            // R2018b stores empty sparse matrices with an empty 'ir' field. We can replace
            // this with a single number store to match the specified behavior.
            rowIndices = new UniversalNumberStore(Int32, bufferAllocator.allocate(4), bufferAllocator);
        }

        // Subfield 5: Column Index (jc)
        NumberStore colIndices = readAsNumberStore(readTagWithExpectedType(Int32));

        // Subfield 6: Real part (pr)
        NumberStore real = readAsNumberStore(readTag());

        // Subfield 7: Imaginary part (pi)
        NumberStore imaginary = null;
        if (header.isComplex()) {
            imaginary = readAsNumberStore(readTag());
        }

        return createSparse(header.getDimensions(), header.isLogical(),
                header.getNzMax(), real, imaginary, rowIndices, colIndices);
    }

    private Array readChar(EntryHeader header) throws IOException {

        // Subfield 4: Data
        Mat5Tag tag = readTag();
        CharEncoding encoding = tag.getType().getCharEncoding();
        CloseableCharBuffer buffer = encoding.readCharBuffer(source, tag.getNumBytes(), bufferAllocator);
        source.skip(tag.getPadding());
        return createChar(header.getDimensions(), encoding, buffer);

    }

    protected Array readCell(EntryHeader header) throws IOException {

        // Subfield 4: Array of Cells Subelements. Stored in column major order
        final Array[] contents = new Array[header.getNumElements()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = readNestedArray();
        }

        return createCell(header.getDimensions(), contents);
    }

    private Array readStruct(EntryHeader header) throws IOException {
        // Struct has the same structure as an object without a name
        return readStructOrObject(header, null);
    }

    private Array readObject(EntryHeader header) throws IOException {
        // Subfield 4: Class name
        String className = readAsAscii(readTagWithExpectedType(Int8));
        return readStructOrObject(header, className);
    }

    private Array readStructOrObject(EntryHeader header, String objectClassName) throws IOException {
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
        final Array[][] values = readValues(header, numFields, names);

        if (objectClassName == null)
            return createStruct(header.getDimensions(), names, values);
        return createObject(header.getDimensions(), objectClassName, names, values);
    }

    protected Array[][] readValues(EntryHeader header, int numFields, String[] names) throws IOException {
        // Subfield 6/7: Fields ([f f f f ...] * cols * rows)
        int numElements = header.getNumElements();
        final Array[][] values = new Array[numFields][numElements];
        for (int i = 0; i < numElements; i++) {
            for (int field = 0; field < numFields; field++) {
                values[field][i] = readNestedArray();
            }
        }
        return values;
    }

    private Array readFunctionHandle(EntryHeader header) throws IOException {
        Struct content = (Struct) readNestedArray();
        return new MatFunction(content);
    }

    /**
     * Opaque types are used to store various non-covered types such as function handles,
     * tables, and strings. They use a slightly different structure than other types, so
     * we can't share the same header (e.g. there is no dimension array).
     * <p>
     * Note that this is not in the official documentation. The implementation is based
     * on MatFileRW's MatFileReader and personal tests.
     */
    private MatFile.Entry readOpaque(int[] arrayFlags) throws IOException {
        boolean isGlobal = Mat5ArrayFlags.isGlobal(arrayFlags);

        // Subfield 2: Ascii variable name
        String name = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 3: Used to store the object type identifier, e.g., "MCOS" or "handle"
        String objectType = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 4: Class Name, e.g., "table" or "string"
        String className = readAsAscii(readTagWithExpectedType(Int8));

        // Subfield 5: Content
        Array content = readNestedArray();

        return new MatFile.Entry(name, isGlobal, createOpaque(objectType, className, content));

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

    protected Mat5Tag readTagWithExpectedType(Mat5Type expected) throws IOException {
        Mat5Tag tag = readTag();
        if (tag.getType() != expected)
            throw readError("Encountered unexpected tag. Expected %s, Found %s", expected, tag.getType());
        return tag;
    }

    static IOException readError(String format, Object... args) {
        return new IOException(String.format(format, args));
    }

    // ------------------------- Factory methods

    protected Mat5Reader createChildReader(Source source) {
        Mat5Reader reader = new Mat5Reader(source);
        reader.filter = this.filter;
        reader.mcos = this.mcos;
        reader.bufferAllocator = this.bufferAllocator;
        return reader;
    }

    private Matrix createMatrix(int[] dimensions, MatlabType type, boolean logical, NumberStore real, NumberStore imaginary) {
        return new MatMatrix(dimensions, type, logical, real, imaginary);
    }

    private Sparse createSparse(int[] dimensions, boolean logical, int nzMax, NumberStore real, NumberStore imaginary, NumberStore rowIndices, NumberStore colIndices) {
        return new MatSparseCSC(dimensions, logical, nzMax, real, imaginary, rowIndices, colIndices);
    }

    private Char createChar(int[] dims, CharEncoding encoding, CloseableCharBuffer buffer) {
        return new MatChar(dims, encoding, buffer);
    }

    protected Cell createCell(int[] dims, Array[] contents) {
        return new MatCell(dims, contents);
    }

    private Struct createStruct(int[] dims, String[] names, Array[][] values) {
        return new MatStruct(dims, names, values);
    }

    private ObjectStruct createObject(int[] dims, String className, String[] names, Array[][] values) {
        return new MatObjectStruct(dims, className, names, values);
    }

    private Opaque createOpaque(String objectType, String className, Array content) {
        // Serialized Java object
        if ("java".equals(objectType))
            return new MatJavaObject(className, content);

        // MCOS (Matlab Class Object System) related entries
        if ("MCOS".equals(objectType)) {
            if ("FileWrapper__".equals(className)) {
                // Special element that contains data for reference objects
                return new McosFileWrapper(objectType, className, content, source.order());
            } else {
                // Pointer to a reference object
                return mcos.register(McosReference.parseOpaque(objectType, className, content));
            }
        }

        // Generic Opaque object
        return new MatOpaque(objectType, className, content);
    }

    protected Mat5Reader(Source source) {
        this.source = checkNotNull(source, "Source can't be empty");
    }

    protected final Source source;

    private int numEntries = 0;
    private long subsysPosition = Long.MIN_VALUE;
    private boolean nextIsSubsys = false;
    private boolean mayFilterNext = false;
    private boolean reducedHeader = false;
    protected EntryFilter filter = null;
    private ExecutorService executorService = null;
    private boolean processSubsystem = true;
    private int maxInflateBufferSize = 2048;
    protected McosRegistry mcos = new McosRegistry();
    protected BufferAllocator bufferAllocator = Mat5.getDefaultBufferAllocator();

}
