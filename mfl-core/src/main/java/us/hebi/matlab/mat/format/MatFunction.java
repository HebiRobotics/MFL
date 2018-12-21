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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.*;

import java.io.IOException;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * @author Florian Enner
 * @since 02 Sep 2018
 */
class MatFunction extends AbstractArray implements FunctionHandle, Mat5Serializable {

    MatFunction(Struct content) {
        super(new int[]{1, 1});
        this.content = content;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Function;
    }

    @Override
    public Struct getContent() {
        return content;
    }

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + computeArraySize(getContent());
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);
        writeNestedArray(content, sink);
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    final Struct content;

    @Override
    protected int subHashCode() {
        return content.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        MatFunction other = (MatFunction) otherGuaranteedSameClass;
        return other.content.equals(content);
    }
}
