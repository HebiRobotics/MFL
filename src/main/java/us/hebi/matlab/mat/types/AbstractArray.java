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

import static us.hebi.matlab.mat.util.Preconditions.*;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 02 May 2018
 */
public abstract class AbstractArray implements Array {

    @Override
    public int[] getDimensions() {
        return dims;
    }

    @Override
    public int getNumDimensions() {
        return getDimensions().length;
    }

    @Override
    public int getNumRows() {
        return getDimensions()[0];
    }

    @Override
    public int getNumCols() {
        return getDimensions()[1];
    }

    @Override
    public int getNumElements() {
        return getNumElements(getDimensions());
    }

    @Override
    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    /**
     * @return cumulative product, i.e. number of elements given dimensions
     */
    public static int getNumElements(int[] dimensions) {
        int count = dimensions[0];
        for (int i = 1; i < dimensions.length; i++) {
            count *= dimensions[i];
        }
        return count;
    }

    protected static int[] calculateColMajorStrides(int[] dimensions) {
        int[] dimStrides = new int[dimensions.length];
        dimStrides[0] = 1;
        for (int i = 0; i < dimStrides.length - 1; i++) {
            dimStrides[i + 1] = dimensions[i] * dimStrides[i];
        }
        return dimStrides;
    }

    protected int getColumnMajorIndex(int row, int col) {
        checkNumDimensions(2);
        final int ix0 = checkIndexBounds(row, 0);
        final int ix1 = checkIndexBounds(col, 1);
        return ix0 * dimStrides[0] + ix1 * dimStrides[1];
    }

    protected int getColumnMajorIndex(int[] indices) {
        checkNumDimensions(indices.length);
        int index = 0;
        for (int i = 0; i < indices.length; i++) {
            index += dimStrides[i] * checkIndexBounds(indices[i], i);
        }
        return index;
    }

    protected void checkNumDimensions(int numDim) {
        if (numDim != dimStrides.length)
            throw new IllegalArgumentException("Expected " + dimStrides.length + " dimensions. Found: " + numDim);
    }

    protected int checkIndexBounds(int index, int dim) {
        if (index >= 0 && index < dims[dim])
            return index;
        String msg = String.format("Index exceeds matrix dimension %d. %d/%d", dim, index, dims[dim] - 1);
        throw new IllegalArgumentException(msg);
    }

    protected AbstractArray(int[] dims, boolean isGlobal) {
        this.dims = checkNotNull(dims);
        checkArgument(dims.length >= 2, "Every array must have at least two dimensions");
        this.dimStrides = calculateColMajorStrides(dims);
    }

    @Override
    public String toString() {
        return StringHelper.toString(this);
    }

    // Common Variables
    protected final int[] dims;
    private boolean isGlobal;

    // Internal state
    private final int[] dimStrides;

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(dims);
        result = prime * result + (isGlobal ? 1231 : 1237);
        result = prime * result + subHashCode();
        return result;
    }

    @Override
    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null) {
            return false;
        } else if (other.getClass().equals(this.getClass())) {
            AbstractArray otherArray = (AbstractArray) other;
            return otherArray.isGlobal == isGlobal &&
                    Arrays.equals(otherArray.dims, dims) &&
                    subEqualsGuaranteedSameClass(other);
        } else {
            return false;
        }
    }

    protected abstract int subHashCode();

    protected abstract boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass);
}
