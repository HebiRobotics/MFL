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

package us.hebi.matlab.mat.tests.serialization.ejml;

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
class DMatrixSparseCSCWrapper extends AbstractArray implements Mat5Serializable, Mat5Serializable.Mat5Attributes {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + Int32.computeSerializedSize(getNumRowIndices())
                + Int32.computeSerializedSize(getNumColIndices())
                + Double.computeSerializedSize(getNzMax());
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);

        // Row indices (MATLAB requires at least 1 entry)
        Int32.writeTag(getNumRowIndices(), sink);
        if (matrix.getNonZeroLength() == 0) {
            sink.writeInt(0);
        } else {
            sink.writeInts(matrix.nz_rows, 0, getNumRowIndices());
        }
        Int32.writePadding(getNumRowIndices(), sink);

        // Column indices
        Int32.writeTag(getNumColIndices(), sink);
        sink.writeInts(matrix.col_idx, 0, getNumColIndices());
        Int32.writePadding(getNumColIndices(), sink);

        // Non-zero values
        Double.writeTag(getNzMax(), sink);
        sink.writeDoubles(matrix.nz_values, 0, getNzMax());
        Double.writePadding(getNzMax(), sink);

    }

    @Override
    public MatlabType getType() {
        return MatlabType.Sparse;
    }

    private int getNumRowIndices() {
        return Math.max(1, matrix.getNonZeroLength());
    }

    private int getNumColIndices() {
        return matrix.getNumCols() + 1;
    }

    @Override
    public int getNzMax() {
        return matrix.getNonZeroLength();
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
        dims[0] = matrix.getNumRows();
        dims[1] = matrix.getNumCols();
        return dims;
    }

    DMatrixSparseCSCWrapper(DMatrixSparseCSC matrix) {
        super(Mat5.dims(matrix.getNumRows(), matrix.getNumCols()));
        if (!matrix.indicesSorted)
            throw new IllegalArgumentException("Indices must be sorted!");
        this.matrix = matrix;
    }

    @Override
    public void close() throws IOException {
    }

    final DMatrixSparseCSC matrix;

    @Override
    protected int subHashCode() {
        return matrix.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        DMatrixSparseCSCWrapper other = (DMatrixSparseCSCWrapper) otherGuaranteedSameClass;
        return other.matrix.equals(matrix);
    }

}
