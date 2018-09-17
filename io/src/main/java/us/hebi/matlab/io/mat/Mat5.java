package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.util.Casts;
import us.hebi.matlab.io.types.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

import static us.hebi.matlab.io.types.AbstractArray.*;

/**
 * Utility methods for creating Matlab classes that are compatible with
 * the Mat 5 file format.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
public class Mat5 {

    // ------------------------- Common methods for omitting sources/sinks with default configurations

    public static File writeToFile(MatFile matFile, String fileName) throws IOException {
        return writeToFile(matFile, new File(fileName));
    }

    public static File writeToFile(MatFile matFile, File file) throws IOException {
        writeToSink(matFile, Sinks.newStreamingFile(file));
        return file;
    }

    public static ByteBuffer writeToBuffer(MatFile matFile) {
        // Allocate a reasonably sized buffer
        long capacity = Mat5Writer.computeUncompressedSize(matFile);
        capacity += 1024; // safety margin in case compressed files end up larger (small matrices!)
        ByteBuffer buffer = allocateBuffer(Casts.sint32(capacity));
        buffer.order(DEFAULT_ORDER);

        // Write to buffer
        try {
            writeToSink(matFile, Sinks.wrap(buffer));
            return buffer;
        } catch (IOException ioe) {
            throw new RuntimeException("Encountered IOException when writing to buffer", ioe);
        }
    }

    private static void writeToSink(MatFile matFile, Sink sink) throws IOException {
        try {
            matFile.writeTo(sink);
        } finally {
            sink.close();
        }
    }

    public static Mat5File readFromFile(String fileName) throws IOException {
        return readFromFile(new File(fileName));
    }

    public static Mat5File readFromFile(File file) throws IOException {
        return readFromSource(Sources.openFile(file));
    }

    public static Mat5File readFromBuffer(ByteBuffer buffer) {
        try {
            return readFromSource(Sources.wrap(buffer));
        } catch (IOException ioe) {
            throw new RuntimeException("Encountered IOException when reading from buffer", ioe);
        }
    }

    private static Mat5File readFromSource(Source source) throws IOException {
        try {
            return newReader(source).readFile();
        } finally {
            source.close();
        }
    }

    // ------------------------- User-facing Factory API

    public static Mat5File newMatFile() {
        return new Mat5File();
    }

    public static Mat5Reader newReader(Source source) {
        return new Mat5Reader(source);
    }

    public static Mat5Writer newWriter(Sink sink) {
        return new Mat5Writer(sink);
    }

    public static int[] index(int rows, int cols) {
        return new int[]{rows, cols};
    }

    public static int[] index(int rows, int cols, int... other) {
        int[] dims = new int[other.length + 2];
        dims[0] = rows;
        dims[1] = cols;
        System.arraycopy(other, 0, dims, 2, other.length);
        return dims;
    }

    public static int[] dims(int rows, int cols) {
        return index(rows, cols);
    }

    public static int[] dims(int rows, int cols, int... other) {
        return index(rows, cols, other);
    }

    public static Cell newCell(int rows, int cols) {
        return newCell(dims(rows, cols));
    }

    public static Cell newCell(int[] dims) {
        return new MatCell(dims, false);
    }

    public static Struct newStruct() {
        return newStruct(1, 1);
    }

    public static Struct newStruct(int rows, int cols) {
        return newStruct(dims(rows, cols));
    }

    public static Struct newStruct(int[] dims) {
        return new MatStruct(dims, false);
    }

    public static Char newChar(int rows, int cols) {
        return newChar(dims(rows, cols));
    }

    public static Char newChar(int rows, int cols, CharEncoding encoding) {
        return new MatChar(dims(rows, cols), encoding);
    }

    public static Char newChar(int[] dims) {
        return newChar(dims, CharEncoding.Utf8);
    }

    public static Char newChar(int[] dims, CharEncoding encoding) {
        return new MatChar(dims, encoding);
    }

    /**
     * Creates a column character vector identical to the
     * single quoted 'string' representation in MATLAB
     *
     * @param value
     * @return
     */
    public static Char newString(String value) {
        return newString(value, CharEncoding.Utf8);
    }

    public static Char newString(String value, CharEncoding encoding) {
        return new MatChar(new int[]{1, value.length()}, false, encoding, CharBuffer.wrap(value));
    }


    public static Matrix newLogicalScalar(boolean value) {
        Matrix logical = newLogical(1, 1);
        logical.setBoolean(0, value);
        return logical;
    }

    public static Matrix newScalar(double value) {
        Matrix matrix = newMatrix(1, 1);
        matrix.setDouble(0, 0, value);
        return matrix;
    }

    public static Matrix newComplexScalar(double real, double imaginary) {
        Matrix complex = newComplex(1, 1);
        complex.setDouble(0, real);
        complex.setImaginaryDouble(0, imaginary);
        return complex;
    }

    public static Matrix newLogical(int rows, int cols) {
        return newLogical(dims(rows, cols));
    }

    public static Matrix newLogical(int[] dims) {
        return createMatrix(dims, MatlabType.Int8, true, false);
    }

    public static Matrix newMatrix(int rows, int cols) {
        return newMatrix(dims(rows, cols));
    }

    public static Matrix newMatrix(int[] dims) {
        return newMatrix(dims, MatlabType.Double);
    }

    public static Matrix newMatrix(int rows, int cols, MatlabType type) {
        return newMatrix(dims(rows, cols), type);
    }

    public static Matrix newMatrix(int[] dims, MatlabType type) {
        return createMatrix(dims, type, false, false);
    }

    public static Matrix newComplex(int rows, int cols) {
        return newComplex(dims(rows, cols));
    }

    public static Matrix newComplex(int rows, int cols, MatlabType type) {
        return newComplex(dims(rows, cols), type);
    }

    public static Matrix newComplex(int[] dims) {
        return newComplex(dims, MatlabType.Double);
    }

    public static Matrix newComplex(int[] dims, MatlabType type) {
        return createMatrix(dims, type, false, true);
    }

    private static Matrix createMatrix(int[] dims, MatlabType type, boolean logical, boolean complex) {
        return new MatMatrix(dims, false, type, logical,
                createStore(type, dims), complex ? createStore(type, dims) : null);
    }

    private static NumberStore createStore(MatlabType type, int[] dims) {
        Mat5Type tagType = Mat5Type.fromNumericalType(type);
        ByteBuffer buffer = allocateBuffer(getNumElements(dims) * tagType.bytes());
        buffer.order(DEFAULT_ORDER);
        return new UniversalNumberStore(tagType, buffer);
    }

    static ByteBuffer allocateBuffer(int numBytes) {
        if (numBytes <= 16 * 1024) // on-heap threshold
            return ByteBuffer.allocate(numBytes);
        return ByteBuffer.allocateDirect(numBytes);
    }

    /**
     * Currently just used internally for matrices that contain
     * binary data that needs to be parsed (e.g. subsystem or
     * Java serialization)
     *
     * @param matrix
     * @return
     */
    static ByteBuffer exportBytes(Matrix matrix) {
        if (matrix instanceof MatMatrix) {
            MatMatrix matMatrix = (MatMatrix) matrix;
            if (matMatrix.getRealStore() instanceof UniversalNumberStore)
                return ((UniversalNumberStore) matMatrix.getRealStore()).getByteBuffer();
        }
        throw new IllegalStateException("Not implemented for input type");
    }

    public static final Array EMPTY_MATRIX = newMatrix(0, 0);
    public static final ByteOrder DEFAULT_ORDER = ByteOrder.nativeOrder();

    private Mat5() {
    }

    public static final int MATRIX_TAG_SIZE = 8; // no padding as all sub-fields are already padded
    public static final int FILE_HEADER_SIZE = 116 + 8 + 2 + 2;
    public static final int REDUCED_FILE_HEADER_SIZE = 2 + 2 + 4 /* padding */;

}
