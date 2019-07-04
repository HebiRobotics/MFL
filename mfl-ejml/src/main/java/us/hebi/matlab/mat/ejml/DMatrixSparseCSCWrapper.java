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

package us.hebi.matlab.mat.ejml;

import org.ejml.data.DMatrixSparseCSC;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * Serializes an EJML Sparse CSC double matrix
 *
 * @author Florian Enner
 */
class DMatrixSparseCSCWrapper extends AbstractSparseWrapper<DMatrixSparseCSC> {

    @Override
    protected int getMat5SparseNonZeroDataSize() {
        return Mat5Type.Double.computeSerializedSize(matrix.getNonZeroLength());
    }

    @Override
    protected void writeMat5SparseNonZeroData(Sink sink) throws IOException {
        // Non-zero values
        Mat5Type.Double.writeTag(matrix.getNonZeroLength(), sink);
        sink.writeDoubles(matrix.nz_values, 0, matrix.getNonZeroLength());
        Mat5Type.Double.writePadding(matrix.getNonZeroLength(), sink);
    }

    @Override
    protected int[] getRowIndices() {
        return matrix.nz_rows;
    }

    @Override
    protected int[] getColIndices() {
        return matrix.col_idx;
    }

    DMatrixSparseCSCWrapper(DMatrixSparseCSC matrix) {
        super(matrix);
        if (!matrix.indicesSorted) {
            matrix.sortIndices(null);
        }
    }

}
