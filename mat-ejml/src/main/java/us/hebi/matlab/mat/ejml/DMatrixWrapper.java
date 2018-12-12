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

import org.ejml.data.DMatrix;
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
 * Serializes an EJML double matrix into a MAT 5 file that can be read by MATLAB
 *
 * @author Florian Enner
 */
class DMatrixWrapper extends AbstractArray implements Mat5Serializable {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + Mat5Type.Double.computeSerializedSize(matrix.getNumElements());
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);

        // Data in column major format
        Mat5Type.Double.writeTag(matrix.getNumElements(), sink);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                sink.writeDouble(matrix.unsafe_get(row, col));
            }
        }
        Mat5Type.Double.writePadding(matrix.getNumElements(), sink);

    }

    @Override
    public MatlabType getType() {
        return MatlabType.Double;
    }

    @Override
    public int[] getDimensions() {
        dims[0] = matrix.getNumRows();
        dims[1] = matrix.getNumCols();
        return dims;
    }

    DMatrixWrapper(DMatrix matrix) {
        super(Mat5.dims(matrix.getNumRows(), matrix.getNumCols()));
        this.matrix = matrix;
    }

    @Override
    public void close() throws IOException {
    }

    final DMatrix matrix;

    @Override
    protected int subHashCode() {
        return matrix.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        DMatrixWrapper other = (DMatrixWrapper) otherGuaranteedSameClass;
        return other.matrix.equals(matrix);
    }

}
