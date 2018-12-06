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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.AbstractCell;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;
import java.util.Arrays;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * @author Florian Enner
 * @since 29 Aug 2018
 */
class MatCell extends AbstractCell implements Mat5Serializable {

    MatCell(int[] dims, boolean global) {
        super(dims, global, new Array[getNumElements(dims)]);
        Arrays.fill(contents, getEmptyValue());
    }

    MatCell(int[] dims, boolean global, Array[] contents) {
        super(dims, global, contents);
    }

    @Override
    protected Array getEmptyValue() {
        return Mat5.EMPTY_MATRIX;
    }

    @Override
    public int getMat5Size(String name) {
        int size = Mat5.MATRIX_TAG_SIZE;
        size += computeArrayHeaderSize(name, this);
        for (int i = 0; i < getNumElements(); i++) {
            size += computeArraySize(get(i));
        }
        return size;
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        for (int i = 0; i < getNumElements(); i++) {
            writeArrayWithTag(get(i), sink);
        }
    }

    @Override
    protected int subHashCode() {
        return Arrays.hashCode(contents);
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        MatCell other = (MatCell) otherGuaranteedSameClass;
        return Arrays.equals(other.contents, contents);
    }
}
