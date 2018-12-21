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

import org.ejml.data.FMatrix;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * Serializes an EJML float matrix
 *
 * @author Florian Enner
 */
class FMatrixWrapper extends AbstractMatrixWrapper<FMatrix> {

    @Override
    protected int getMat5DataSize() {
        return Mat5Type.Single.computeSerializedSize(matrix.getNumElements());
    }

    @Override
    protected void writeMat5Data(Sink sink) throws IOException {
        // Real data in column major format
        Mat5Type.Single.writeTag(matrix.getNumElements(), sink);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                sink.writeFloat(matrix.unsafe_get(row, col));
            }
        }
        Mat5Type.Single.writePadding(matrix.getNumElements(), sink);
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Single;
    }

    FMatrixWrapper(FMatrix matrix) {
        super(matrix);
    }

}
