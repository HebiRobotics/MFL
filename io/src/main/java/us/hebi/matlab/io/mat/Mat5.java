package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.memory.NativeMemory;
import us.hebi.matlab.io.types.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

import static us.hebi.matlab.io.types.AbstractArray.*;

/**
 * Utility methods for creating Matlab classes that are compatible with
 * the Mat 5 file format.
 *
 * @author Florian Enner
 * @since 29 Aug 2018
 */
public class Mat5 {

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
     * @param value input
     * @return Char array
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

    public static int getSerializedSize(String name, Array array) {
        if (array instanceof Mat5Serializable) {
            return ((Mat5Serializable) array).getMat5Size(name);
        }
        throw new IllegalArgumentException("Array does not support the MAT5 format");
    }

    private static Matrix createMatrix(int[] dims, MatlabType type, boolean logical, boolean complex) {
        return new MatMatrix(dims, false, type, logical,
                createStore(type, dims), complex ? createStore(type, dims) : null);
    }

    private static NumberStore createStore(MatlabType type, int[] dims) {
        Mat5Type tagType = Mat5Type.fromNumericalType(type);
        return new UniversalNumberStore(tagType, getNumElements(dims), getDefaultBufferAllocator());
    }

    static BufferAllocator getDefaultBufferAllocator() {
        return DEFAULT_BUFFER_ALLOCATOR;
    }

    /**
     * Buffer allocator that gets used for all arrays, readers, and writers created
     * in this class. If we find a valid use case for changing this, this may become
     * settable in the future.
     */
    private final static BufferAllocator DEFAULT_BUFFER_ALLOCATOR = new BufferAllocator() {
        @Override
        public ByteBuffer allocate(int numBytes) {
            if (numBytes <= 4096) // (arbitrary) threshold for staying on-heap
                return ByteBuffer.allocate(numBytes);
            return ByteBuffer.allocateDirect(numBytes);
        }

        @Override
        public void release(ByteBuffer buffer) {
            if (buffer.isDirect()) {
                NativeMemory.freeDirectBuffer(buffer);
            }
        }
    };

    /**
     * Currently just used internally for matrices that contain
     * binary data that needs to be parsed (e.g. subsystem or
     * Java serialization)
     * <p>
     * The buffer may be a direct buffer that may be closed by
     * the holding Matrix, so the buffer should not be held on to
     * or go into user managed space!
     *
     * @param matrix matrix
     * @return internal matrix buffer
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
