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

import us.hebi.matlab.mat.types.*;
import us.hebi.matlab.mat.util.Compat;

import java.io.IOException;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * @author Florian Enner
 * @since 04 Sep 2018
 */
class MatOpaque extends AbstractArray implements Opaque, Mat5Serializable {

    MatOpaque(String objectType, String className, Array content) {
        super(SINGLE_DIM);
        this.content = content;
        this.className = className;
        this.objectType = objectType;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Opaque;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getClassName() {
        return className;
    }

    public Array getContent() {
        return content;
    }

    @Override
    public int getMat5Size(String name) {
        return computeOpaqueSize(name, this);
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeOpaque(name, isGlobal, this, sink);
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    private final String objectType;
    private final String className;
    private final Array content;
    private static final int[] SINGLE_DIM = new int[]{1, 1};

    @Override
    protected int subHashCode() {
        return Compat.hash(objectType, className, content);
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        MatOpaque other = (MatOpaque) otherGuaranteedSameClass;
        return other.objectType.equals(objectType) &&
                other.className.equals(className) &&
                other.content.equals(content);
    }
}
