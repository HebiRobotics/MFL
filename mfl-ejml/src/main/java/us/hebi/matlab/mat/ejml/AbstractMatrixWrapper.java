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

import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5Serializable;
import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * Serializes an EJML Matrix into a MAT 5 file that can be read by MATLAB
 *
 * Note that implementing 'Mat5Attributes' lets us get around the overhead of
 * implementing the Matrix / Sparse interfaces, or alternatively writing the
 * header manually.
 *
 * @author Florian Enner
 * @since 13 Dec 2018
 */
abstract class AbstractMatrixWrapper<M extends org.ejml.data.Matrix> extends AbstractArray implements Mat5Serializable, Mat5Serializable.Mat5Attributes {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + getMat5DataSize();
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);
        writeMat5Data(sink);
    }

    protected abstract int getMat5DataSize();

    /**
     * Writes data part in column-major order
     *
     * @param sink
     * @throws IOException
     */
    protected abstract void writeMat5Data(Sink sink) throws IOException;

    @Override
    public boolean isLogical() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    @Override
    public int getNzMax() {
        return 0;
    }

    @Override
    public int[] getDimensions() {
        dims[0] = matrix.getNumRows();
        dims[1] = matrix.getNumCols();
        return dims;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    protected int subHashCode() {
        return matrix.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        CMatrixWrapper other = (CMatrixWrapper) otherGuaranteedSameClass;
        return other.matrix.equals(matrix);
    }

    protected AbstractMatrixWrapper(M matrix) {
        super(Mat5.dims(matrix.getNumRows(), matrix.getNumCols()));
        this.matrix = matrix;
    }

    protected final M matrix;

}
