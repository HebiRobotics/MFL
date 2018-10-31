package us.hebi.matlab.mat.format.mat5test;

import org.junit.Test;
import us.hebi.matlab.mat.types.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.util.Casts.*;
import static us.hebi.matlab.mat.format.Mat5.*;
import static us.hebi.matlab.mat.format.mat5test.MatTestUtil.*;

/**
 * Most tests and data copied from JMatIO / MatFileRW
 *
 * @author Florian Enner
 * @since 04 May 2018
 */
public class ArrayReadTest {

    @Test
    public void testReadingNaN() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/nan.mat").getMatrix("x");
        assertTrue(Double.isNaN(real.getDouble(0, 0)));
    }

    @Test
    public void testSingle() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/single.mat").getMatrix("arr");
        assertEquals(MatlabType.Single, real.getType());
        for (int i = 0; i < 3; i++) {
            float desired = (i + 1) * 1.1f;
            assertEquals(desired, real.getFloat(0, i), FLOAT_DELTA);
        }
    }

    @Test
    public void testUInt8() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/uint8.mat").getMatrix("arr");
        assertEquals(MatlabType.UInt8, real.getType());
        assertEquals(0, uint8(real.getByte(0, 0)));
        assertEquals(255, uint8(real.getByte(0, 1)));
    }

    @Test
    public void testUInt32Array() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/uint32.mat").getMatrix("a");
        assertEquals(MatlabType.UInt32, real.getType());
        assertArrayEquals(new int[]{1, 4}, real.getDimensions());
        assertArrayEquals(new long[]{1, 2, 3, 4}, new long[]{
                uint32(real.getInt(0, 0)),
                uint32(real.getInt(0, 1)),
                uint32(real.getInt(0, 2)),
                uint32(real.getInt(0, 3))
        });
    }

    @Test
    public void testUInt64() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/uint64.mat").getMatrix("arr");
        assertEquals(MatlabType.UInt64, real.getType());
        assertEquals(0, real.getLong(0, 0));
        assertEquals(Long.MAX_VALUE, real.getLong(0, 1));
    }

    @Test
    public void testInt8() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/int8.mat").getMatrix("arr");
        assertEquals(MatlabType.Int8, real.getType());
        assertEquals(Byte.MIN_VALUE, real.getByte(0, 0));
        assertEquals(Byte.MAX_VALUE, real.getByte(0, 1));
    }

    @Test
    public void testInt32Array() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/int32.mat").getMatrix("a");
        assertEquals(MatlabType.Int32, real.getType());
        assertArrayEquals(new int[]{1, 4}, real.getDimensions());
        assertArrayEquals(new int[]{1, 2, 3, 4}, new int[]{
                real.getInt(0, 0),
                real.getInt(0, 1),
                real.getInt(0, 2),
                real.getInt(0, 3)
        });
    }

    @Test
    public void testInt64() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/int64.mat").getMatrix("arr");
        assertEquals(MatlabType.Int64, real.getType());
        assertEquals(Long.MIN_VALUE, real.getLong(0, 0));
        assertEquals(Long.MAX_VALUE, real.getLong(0, 1));
    }

    @Test
    public void testEmptyBigSparseFile() throws Exception {
        // Note: the index array is quite large, so deactivate round trip to
        // keep unit tests speedy
        MatFile matFile = MatTestUtil.readMat("arrays/bigsparse.mat", false);
        Sparse sparse = matFile.getSparse("s");
        assertArrayEquals(new int[]{1000000, 10000000}, sparse.getDimensions());
        assertEquals(1, sparse.getNzMax()); // for some reason nzMax is always >0
        assertEquals(0, sparse.getLong(10000, 18000));
    }

    @Test
    public void testDoubleFromMatlabCreatedFile() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/matnativedouble.mat").getMatrix("arr");
        assertEquals(MatlabType.Double, real.getType());
        assertArrayEquals(new int[]{3, 2}, real.getDimensions());

        double[] desired = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        double[] actual = new double[desired.length];
        int i = 0;
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                actual[i++] = real.getDouble(row, col);
            }
        }
        assertArrayEquals(desired, actual, DELTA);
    }

    @Test
    public void testDoubleFromMatlabCreatedFile2() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/matnativedouble2.mat").getMatrix("arr");
        assertEquals(MatlabType.Double, real.getType());
        assertArrayEquals(new int[]{3, 2}, real.getDimensions());

        double[] desired = new double[]{1.1, 2.2, 3.3, 4.4, 5.5, 6.6};
        double[] actual = new double[desired.length];
        int i = 0;
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                actual[i++] = real.getDouble(row, col);
            }
        }
        assertArrayEquals(desired, actual, DELTA);
    }

    @Test
    public void testNestedStructuresFromMatlabCreatedFile() throws Exception {
        // Note: I'm not sure what the original 'cell' test was supposed to do, but
        // the mat file contains a lot of nested structures that are a good
        // parsing sanity check
        Cell name = MatTestUtil.readMat("arrays/cell.mat")
                .getStruct("cel")
                .getStruct("xX")
                .getCell("name");

        assertEquals("Sn(1) test*bf(1)", name.getChar(0).getString());
        assertEquals("Sn(1) test*bf(2)", name.getChar(1).getString());
        assertEquals("Sn(1) constant", name.getChar(2).getString());

    }

    @Test
    public void testMultipleDimArrayComplexFromMatlabCreatedFile() throws Exception {
        Matrix complex = MatTestUtil.readMat("arrays/multiDimComplexMatrix.mat").getMatrix("in");
        assertEquals(MatlabType.Double, complex.getType());
        int[] dims = new int[]{2, 3, 4, 5, 6};
        assertEquals(dims.length, complex.getNumDimensions());
        assertArrayEquals(dims, complex.getDimensions());

        // Access sequentially using multi-dim indexing
        double expected = 0.0;
        for (int i = 0; i < dims[4]; i++) {
            for (int j = 0; j < dims[3]; j++) {
                for (int k = 0; k < dims[2]; k++) {
                    for (int l = 0; l < dims[1]; l++) {
                        for (int m = 0; m < dims[0]; m++, expected += 1) {

                            double real = complex.getDouble(index(m, l, k, j, i));
                            assertEquals(expected, real, DELTA);

                            double imaginary = complex.getImaginaryDouble(index(m, l, k, j, i));
                            assertEquals(-expected, imaginary, DELTA);

                        }
                    }
                }
            }
        }

    }

    @Test
    public void testMultipleDimArrayFromMatlabCreatedFile() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/multiDimMatrix.mat").getArray("in");
        assertEquals(MatlabType.Double, real.getType());
        int[] dims = new int[]{2, 3, 4, 5, 6};
        assertEquals(dims.length, real.getNumDimensions());
        assertArrayEquals(dims, real.getDimensions());

        // Access sequentially using raw store
        int numElements = real.getNumElements();
        for (int i = 0; i < numElements; i++) {
            assertEquals(i, real.getLong(i));
        }

    }

    @Test
    public void testSparseFromMatlabCreatedFile() throws Exception {
        // Load variable
        Sparse sparse = MatTestUtil.readMat("arrays/sparse.mat").getSparse("spa");
        assertFalse(sparse.isLogical());
        assertFalse(sparse.isComplex());
        assertEquals(MatlabType.Sparse, sparse.getType());
        int[] dims = new int[]{5, 6};
        assertEquals(dims.length, sparse.getNumDimensions());
        assertArrayEquals(dims, sparse.getDimensions());

        // Filled values
        assertEquals(1, sparse.getLong(3, 3));
        assertEquals(4, sparse.getLong(4, 5)); // highest index

        // Empty values
        assertEquals(0, sparse.getLong(0, 4));
        assertEquals(0, sparse.getLong(1, 1));
        assertEquals(0, sparse.getLong(3, 2));

        // Empty values with custom default value
        sparse.setDefaultValue(2.5);
        assertEquals(2, sparse.getLong(4, 4)); // (long) default
        assertEquals(2.5, sparse.getDouble(0, 0), DELTA); // lowest index
    }

    @Test
    public void testStructureArrayFromMatlabCreatedFile() throws Exception {
        Struct struct = MatTestUtil.readMat("arrays/simplestruct.mat").getStruct("structure");
        assertEquals(MatlabType.Structure, struct.getType());
        assertEquals(2, struct.getNumElements());
        assertEquals(2, struct.getFieldNames().size());

        Char string = struct.get("string", 0, 0);
        assertEquals("ala ma kota", string.getRow(0));

        Matrix array = struct.get("a", 1, 0);
        assertEquals(6, array.getDouble(1, 2), DELTA);
    }

    @Test
    public void testLogicalArray() throws Exception {
        Matrix real = MatTestUtil.readMat("arrays/logical.mat").getMatrix("bool");
        assertTrue(real.isLogical());
        assertEquals(MatlabType.UInt8, real.getType());
        assertEquals(2, real.getNumElements());
        assertEquals(true, real.getBoolean(0, 0));
        assertEquals(false, real.getBoolean(0, 1));
    }

    @Test
    public void testUTF() throws Exception {
        Struct struct = MatTestUtil.readMat("arrays/utf.mat").getStruct("val");
        Char utf8 = struct.get("utf8");
        Char utf16 = struct.get("utf16");
        Char utf32 = struct.get("utf32");

        String expected = "\uD841\uDF0E";
        assertEquals(expected, utf8.getRow(0));
        assertEquals(expected, utf16.getRow(0));
        assertEquals(expected, utf32.getRow(0));
    }

    @Test
    public void testJavaObject() throws Exception {
        MatFile matFile = MatTestUtil.readMat("arrays/java.mat");
        JavaObject javaObject = matFile.getArray("f");
        assertEquals("java.io.File", javaObject.getClassName());
        assertEquals(new File("c:/temp"), javaObject.instantiateObject());
    }

    @Test
    public void testObject() throws Exception {
        MatFile matFile = MatTestUtil.readMat("arrays/object.mat");
        ObjectStruct object = matFile.getArray("X");
        assertEquals("inline", object.getClassName());
        assertTrue(object.getFieldNames().contains("expr"));
    }

    @Test
    public void testComplexSparse() throws Exception {
        Sparse sparse = MatTestUtil.readMat("arrays/complexSparse.mat").getSparse("complexSparse");
        /*
        complexSparse =
           1.0e+02 *
           ...
           (16,19)     3.1900 + 0.0000i
           (17,19)     3.3900 + 0.0000i
           (19,19)     0.0000 + 0.0100i
           (20,19)     3.9900 + 0.0000i
           (1,20)     3.8100 + 0.0000i
           ...
         */
        sparse.setDefaultValue(-1);
        assertEquals(-1, sparse.getLong(0, 2)); // not set
        assertEquals(0, sparse.getLong(18, 18)); // only imag part

        // Only real part
        assertEquals(319, sparse.getLong(15, 18));
        assertEquals(0, sparse.getImaginaryLong(15, 18));

        // Only imaginary part
        assertEquals(0, sparse.getLong(18, 18));
        assertEquals(1, sparse.getImaginaryLong(18, 18));

    }

    @Test
    public void testSparseIteration() throws Exception {
        /*
         * MATLAB generated values:
         * sp = sparse(8,9)
         * sp(:,3) = 0:7
         * sp(3,:) = 0:8
         */
        final List<SparseValue> expected = new ArrayList<SparseValue>(14);
        expected.add(mlSparse(3, 2, 1));
        expected.add(mlSparse(2, 3, 1));
        expected.add(mlSparse(3, 3, 2));
        expected.add(mlSparse(4, 3, 3));
        expected.add(mlSparse(5, 3, 4));
        expected.add(mlSparse(6, 3, 5));
        expected.add(mlSparse(7, 3, 6));
        expected.add(mlSparse(8, 3, 7));
        expected.add(mlSparse(3, 4, 3));
        expected.add(mlSparse(3, 5, 4));
        expected.add(mlSparse(3, 6, 5));
        expected.add(mlSparse(3, 7, 6));
        expected.add(mlSparse(3, 8, 7));
        expected.add(mlSparse(3, 9, 8));

        // Read sparse from file
        Sparse sparse = MatTestUtil.readMat("arrays/denseRowColSparse.mat").getSparse("sp");
        final List<SparseValue> actual = new ArrayList<SparseValue>(sparse.getNzMax());
        sparse.forEach((row, col, real, imag) -> actual.add(new SparseValue(row, col, real, imag)));

        // Compare values and column-major ordering
        assertEquals(expected, actual);

    }

    // Converts 1 indexed to zero indexed
    private static SparseValue mlSparse(int r, int c, double v) {
        return new SparseValue(r - 1, c - 1, v, 0);
    }

    private static final class SparseValue {
        public SparseValue(int row, int col, double real, double imag) {
            this.row = row;
            this.col = col;
            this.real = real;
            this.imag = imag;
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SparseValue))
                return false;
            final SparseValue other = (SparseValue) obj;
            return row == other.row && col == other.col && real == other.real && imag == other.imag;
        }

        int row, col;
        double real, imag;
    }

    @Test
    public void testEmptyName() throws Exception {
        // There are 3 elements in the file. Make sure that they end up correctly in the output.
        // We don't test round-trip as empty array names aren't legal in MATLAB. The only empty
        // name should be the subsystem
        MatFile matFile = MatTestUtil.readMat("arrays/emptyname.mat", false);
        assertEquals(3, matFile.size());
        assertEquals(1, uint8(matFile.getMatrix(0).getByte(0)));
        assertEquals(2, uint8(matFile.getMatrix(1).getByte(0)));
        assertEquals(3, uint8(matFile.getMatrix(2).getByte(0)));
    }

}
