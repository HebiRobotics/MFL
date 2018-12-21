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

import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.ObjectStruct;

/**
 * @author Florian Enner
 * @since 03 Sep 2018
 */
class MatObjectStruct extends MatStruct implements ObjectStruct {

    MatObjectStruct(int[] dims, String className, String[] names, Array[][] values) {
        super(dims, names, values);
        this.className = className;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Object;
    }

    @Override
    public String getPackageName() {
        return "";
    }

    @Override
    public String getClassName() {
        return className;
    }

    private final String className;

    @Override
    protected int subHashCode() {
        return 31 * className.hashCode() + super.subHashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        MatObjectStruct other = (MatObjectStruct) otherGuaranteedSameClass;
        return other.className.equals(className) && super.subEqualsGuaranteedSameClass(other);
    }
}
