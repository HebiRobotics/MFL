package us.hebi.matlab.io.types;

import java.io.IOException;

import static us.hebi.matlab.common.util.Preconditions.*;

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

}
