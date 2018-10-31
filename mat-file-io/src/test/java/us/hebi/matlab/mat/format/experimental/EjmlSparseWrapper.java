package us.hebi.matlab.mat.format.experimental;

import org.ejml.data.DMatrixSparseCSC;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5Serializable;
import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

import static us.hebi.matlab.mat.format.Mat5Type.Double;
import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * Serializes an EJML Sparse CSC matrix into a MAT 5 file that can be read by MATLAB.
 * <p>
 * The data is stored almost identically, so there isn't much conversion required.
 * Implementing 'Mat5Attributes' lets us get around the overhead of implementing
 * the entire Sparse interface, or alternatively manually writing the header.
 *
 * @author Florian Enner
 */
public class EjmlSparseWrapper extends AbstractArray implements Mat5Serializable, Mat5Serializable.Mat5Attributes {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + Int32.computeSerializedSize(getNumRowIndices())
                + Int32.computeSerializedSize(getNumColIndices())
                + Double.computeSerializedSize(getNzMax());
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);

        // Row indices (MATLAB requires at least 1 entry)
        Int32.writeTag(getNumRowIndices(), sink);
        if (sparse.getNonZeroLength() == 0) {
            sink.writeInt(0);
        } else {
            sink.writeInts(sparse.nz_rows, 0, getNumRowIndices());
        }
        Int32.writePadding(getNumRowIndices(), sink);

        // Column indices
        Int32.writeTag(getNumColIndices(), sink);
        sink.writeInts(sparse.col_idx, 0, getNumColIndices());
        Int32.writePadding(getNumColIndices(), sink);

        // Non-zero values
        Double.writeTag(getNzMax(), sink);
        sink.writeDoubles(sparse.nz_values, 0, getNzMax());
        Double.writePadding(getNzMax(), sink);

    }

    @Override
    public MatlabType getType() {
        return MatlabType.Sparse;
    }

    private int getNumRowIndices() {
        return Math.max(1, sparse.getNonZeroLength());
    }

    private int getNumColIndices() {
        return sparse.getNumCols() + 1;
    }

    @Override
    public int getNzMax() {
        return sparse.getNonZeroLength();
    }

    @Override
    public boolean isLogical() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    public EjmlSparseWrapper(DMatrixSparseCSC sparse) {
        super(Mat5.dims(sparse.getNumRows(), sparse.getNumCols()), false);
        if (!sparse.indicesSorted)
            throw new IllegalArgumentException("Indices must be sorted!");
        this.sparse = sparse;
    }

    @Override
    public void close() throws IOException {
    }

    final DMatrixSparseCSC sparse;

}
