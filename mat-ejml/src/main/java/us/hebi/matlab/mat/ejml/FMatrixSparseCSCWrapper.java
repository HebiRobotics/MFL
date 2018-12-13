/*-
 * #%L
 * Mat-File IO
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

package us.hebi.matlab.mat.ejml;

import org.ejml.data.FMatrixSparseCSC;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5Serializable;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.format.Mat5WriteUtil;
import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

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
class FMatrixSparseCSCWrapper extends AbstractArray implements Mat5Serializable, Mat5Serializable.Mat5Attributes {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + Mat5Type.Int32.computeSerializedSize(getNumRowIndices())
                + Mat5Type.Int32.computeSerializedSize(getNumColIndices())
                + Mat5Type.Single.computeSerializedSize(getNzMax());
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);

        // Row indices (MATLAB requires at least 1 entry)
        Mat5Type.Int32.writeTag(getNumRowIndices(), sink);
        if (sparse.getNonZeroLength() == 0) {
            sink.writeInt(0);
        } else {
            sink.writeInts(sparse.nz_rows, 0, getNumRowIndices());
        }
        Mat5Type.Int32.writePadding(getNumRowIndices(), sink);

        // Column indices
        Mat5Type.Int32.writeTag(getNumColIndices(), sink);
        sink.writeInts(sparse.col_idx, 0, getNumColIndices());
        Mat5Type.Int32.writePadding(getNumColIndices(), sink);

        // Non-zero values
        Mat5Type.Single.writeTag(getNzMax(), sink);
        sink.writeFloats(sparse.nz_values, 0, getNzMax());
        Mat5Type.Single.writePadding(getNzMax(), sink);

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

    @Override
    public int[] getDimensions() {
        dims[0] = sparse.getNumRows();
        dims[1] = sparse.getNumCols();
        return dims;
    }

    public FMatrixSparseCSCWrapper(FMatrixSparseCSC sparse) {
        super(Mat5.dims(sparse.getNumRows(), sparse.getNumCols()));
        if (!sparse.indicesSorted)
            throw new IllegalArgumentException("Indices must be sorted!");
        this.sparse = sparse;
    }

    @Override
    public void close() throws IOException {
    }

    final FMatrixSparseCSC sparse;

    @Override
    protected int subHashCode() {
        return sparse.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        FMatrixSparseCSCWrapper other = (FMatrixSparseCSCWrapper) otherGuaranteedSameClass;
        return other.sparse.equals(sparse);
    }
}
