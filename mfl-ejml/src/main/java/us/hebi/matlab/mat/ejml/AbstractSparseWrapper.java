package us.hebi.matlab.mat.ejml;

import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * @author Florian Enner
 * @since 04 Jul 2019
 */
abstract class AbstractSparseWrapper<M extends org.ejml.data.MatrixSparse> extends AbstractMatrixWrapper<M> {

    AbstractSparseWrapper(M matrix) {
        super(matrix);
    }

    @Override
    protected int getMat5DataSize() {
        return Mat5Type.Int32.computeSerializedSize(getNumRowIndices())
                + Mat5Type.Int32.computeSerializedSize(getNumColIndices())
                + getMat5SparseNonZeroDataSize();
    }

    protected abstract int getMat5SparseNonZeroDataSize();

    @Override
    protected void writeMat5Data(Sink sink) throws IOException {
        // Row indices (always at least 1)
        Mat5Type.Int32.writeTag(getNumRowIndices(), sink);
        sink.writeInts(getRowIndices0(), 0, getNumRowIndices());
        Mat5Type.Int32.writePadding(getNumRowIndices(), sink);

        // Column indices
        Mat5Type.Int32.writeTag(getNumColIndices(), sink);
        sink.writeInts(getColIndices(), 0, getNumColIndices());
        Mat5Type.Int32.writePadding(getNumColIndices(), sink);

        // Data portion
        writeMat5SparseNonZeroData(sink);
    }

    protected abstract void writeMat5SparseNonZeroData(Sink sink) throws IOException;

    @Override
    public MatlabType getType() {
        return MatlabType.Sparse;
    }

    public int getNzMax() {
        // MATLAB sparse are always at least 1 element
        return Math.max(1, matrix.getNonZeroLength());
    }

    private int getNumRowIndices() {
        // Must always match nz max
        return getNzMax();
    }

    private int[] getRowIndices0() {
        // Returns row index array or an empty 1 element array to match nzMax()
        return matrix.getNonZeroLength() == 0 ? EMPTY_ROW_INDEX : getRowIndices();
    }

    private int getNumColIndices() {
        return matrix.getNumCols() + 1;
    }

    protected abstract int[] getRowIndices();

    protected abstract int[] getColIndices();

    private static final int[] EMPTY_ROW_INDEX = new int[1]; // MATLAB expects at least on element

}
