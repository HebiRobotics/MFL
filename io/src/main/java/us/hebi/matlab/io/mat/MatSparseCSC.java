package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.util.Casts;
import us.hebi.matlab.io.types.AbstractSparse;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.io.types.Sparse;

import java.io.IOException;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * The lookup is implemented as a compressed column (CC) sparse
 * matrix, which matches the way that MAT5 files store the data.
 * <p>
 * At the moment this implementation serves as a way to read sparse
 * matrices, but does not allow changing them. The easiest way to
 * add support for creating sparse matrices in the may be to use an
 * intermediate format similar to EJML's DMatrixSparseTriplet to
 * DMatrixSparseCSC conversion.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
class MatSparseCSC extends AbstractSparse implements Sparse, Mat5Serializable {

    MatSparseCSC(int[] dims, boolean global, boolean logical, int nzMax,
                 NumberStore real,
                 NumberStore imaginary,
                 NumberStore rowIndices,
                 NumberStore columnIndices) {
        super(dims, global);

        if (columnIndices.getNumElements() != getNumCols() + 1)
            throw new IllegalArgumentException("Expected (numCols + 1) column indices");
        if (rowIndices.getNumElements() != nzMax)
            throw new IllegalArgumentException("Expected nzMax row indices");
        if (!(real.getNumElements() == nzMax || (nzMax == 1 && real.getNumElements() == 0))) // empty matrices have nzMax = 1
            throw new IllegalArgumentException("Expected data with " + nzMax + " elements");

        this.real = checkNotNull(real);
        this.imaginary = imaginary;
        this.nzMax = nzMax;

        // Flags
        this.complex = imaginary != null;
        this.logical = logical;

        // (N+1) * sizeof(int32)
        this.columnIndices = columnIndices;

        // nzMax * sizeOfDataType
        this.rowIndices = rowIndices;

    }

    @Override
    public boolean isLogical() {
        return logical;
    }

    @Override
    public boolean isComplex() {
        return complex;
    }

    @Override
    public double getDouble(int index) {
        return orLogical(index < 0 ? getDefaultValue() : real.getDouble(index));
    }

    @Override
    public void setDouble(int index, double value) {
        throw new IllegalStateException("This sparse matrix can't be modified.");
    }

    @Override
    public double getImaginaryDouble(int index) {
        if (!complex) return 0;
        return orLogical(index < 0 ? getDefaultValue() : imaginary.getDouble(index));
    }

    @Override
    public void setImaginaryDouble(int index, double value) {
        throw new IllegalStateException("This sparse matrix can't be modified.");
    }

    /**
     * Replace standard index calculation with sparse lookup.
     * <p>
     * The data is stored in a Compressed Column (CC) sparse matrix
     * format. The column index (numCols + 1) array contains the
     * starting point in the row indices for each column.
     * <p>
     * The row indices are sorted, so we can do a binary search
     * within the range of rows
     *
     * @return value index or -1 for empty values
     */
    @Override
    protected int getColumnMajorIndex(int row, int col) {
        // get range of possible row indices
        int fromIndex = Casts.sint32(columnIndices.getLong(col));
        int toIndex = Casts.sint32(columnIndices.getLong(col + 1)); // exclusive
        return searchRowBinary(fromIndex, toIndex, row);
    }

    protected int searchRowLinear(int fromIndex, int toIndex, int row) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (rowIndices.getLong(i) == row)
                return i;
        }
        return -1;
    }

    protected int searchRowBinary(int fromIndex, int toIndex, int row) {
        // binary search for desired row (copied from Arrays::binarySearch0)
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = Casts.sint32(rowIndices.getLong(mid));

            if (midVal < row)
                low = mid + 1;
            else if (midVal > row)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -1;
    }

    @Override
    public int getNzMax() {
        return nzMax;
    }

    @Override
    public void forEach(SparseConsumer action) {
        double imag = 0;

        int i0 = (int) columnIndices.getLong(0);
        for (int col = 0; col < getNumCols(); col++) {
            int i1 = (int) columnIndices.getLong(col + 1);

            for (int i = i0; i < i1; i++) {
                int row = (int) rowIndices.getLong(i);
                if (complex) imag = imaginary.getDouble(i);
                action.accept(row, col, real.getDouble(i), imag);
            }

            i0 = i1;
        }

    }

    @Override
    public void close() throws IOException {
        super.close();
        rowIndices.close();
        columnIndices.close();
    }

    private final NumberStore columnIndices;
    private final NumberStore rowIndices;
    private final NumberStore real;
    private final NumberStore imaginary;
    private final boolean logical;
    private final boolean complex;
    final int nzMax;

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + Mat5Writer.computeArrayHeaderSize(name, this)
                + rowIndices.getMat5Size()
                + columnIndices.getMat5Size()
                + real.getMat5Size()
                + (complex ? imaginary.getMat5Size() : 0);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        rowIndices.writeMat5(sink);
        columnIndices.writeMat5(sink);
        real.writeMat5(sink);
        if (complex) imaginary.writeMat5(sink);
    }

}
