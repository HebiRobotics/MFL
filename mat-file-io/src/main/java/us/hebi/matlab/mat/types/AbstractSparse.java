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

package us.hebi.matlab.mat.types;

/**
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractSparse extends AbstractMatrixBase implements Sparse {

    protected AbstractSparse(int[] dims, boolean isGlobal) {
        super(dims, isGlobal);
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Sparse;
    }

    // --- Remove integer accessors as Sparse are always double matrices

    @Override
    public long getLong(int index) {
        return (long) getDouble(index);
    }

    @Override
    public void setLong(int index, long value) {
        setDouble(index, value);
    }

    @Override
    public long getImaginaryLong(int index) {
        return (long) getImaginaryDouble(index);
    }

    @Override
    public void setImaginaryLong(int index, long value) {
        setImaginaryDouble(index, value);
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(double value) {
        this.defaultValue = value;
    }

    protected double defaultValue = 0;

}
