/*-
 * #%L
 * MAT File Library / EJML
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

import org.ejml.data.BMatrixRMaj;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * Serializes an EJML boolean matrix
 *
 * @author Florian Enner
 */
class BMatrixRMajWrapper extends AbstractMatrixWrapper<BMatrixRMaj> {

    @Override
    protected int getMat5DataSize() {
        return Mat5Type.UInt8.computeSerializedSize(matrix.getNumElements());
    }

    @Override
    protected void writeMat5Data(Sink sink) throws IOException {
        Mat5Type.UInt8.writeTag(matrix.getNumElements(), sink);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                int value = matrix.unsafe_get(row, col) ? 1 : 0;
                sink.writeByte((byte) value);
            }
        }
        Mat5Type.UInt8.writePadding(matrix.getNumElements(), sink);
    }

    @Override
    public MatlabType getType() {
        return MatlabType.UInt8;
    }

    @Override
    public boolean isLogical() {
        return true;
    }

    protected BMatrixRMajWrapper(BMatrixRMaj matrix) {
        super(matrix);
    }

}
