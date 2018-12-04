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

import us.hebi.matlab.mat.util.Casts;
import us.hebi.matlab.mat.types.AbstractSparse;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Sparse;

import java.io.IOException;
import java.util.Objects;

import static us.hebi.matlab.mat.util.Preconditions.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * The lookup is implemented as a compressed column (CC) sparse
 * matrix, which matches the way that MAT5 files store the data.
 * <p>
 * At the moment this implementation serves as a way to read sparse
 * matrices, but does not allow changing them. The easiest way to
 * add support for creating sparse matrices in the may be to use an
 * intermediate format similar to EJML's DMatrixSparseTriplet to
 * DMatrixSparseCSC conversion.
 *
 * @author Florian Enner
 * @since 29 Aug 2018
 */
class MatSparseCSC extends AbstractSparse implements Sparse, Mat5Serializable {

    MatSparseCSC(int[] dims, boolean global, boolean logical, int nzMax,
                 NumberStore real,
                 NumberStore imaginary,
                 NumberStore rowIndices,
                 NumberStore columnIndices) {
        super(dims, global);

        if (columnIndices.getNumElements() != getNumCols() + 1)
            throw new IllegalArgumentException("Expected (numCols + 1) column indices");
        if (rowIndices.getNumElements() != nzMax)
            throw new IllegalArgumentException("Expected nzMax row indices");
        if (!(real.getNumElements() == nzMax || (nzMax == 1 && real.getNumElements() == 0))) // empty matrices have nzMax = 1
            throw new IllegalArgumentException("Expected data with " + nzMax + " elements");

        this.real = checkNotNull(real);
        this.imaginary = imaginary;
        this.nzMax = nzMax;

        // Flags
        this.complex = imaginary != null;
        this.logical = logical;

        // (N+1) * sizeof(int32)
        this.columnIndices = columnIndices;

        // nzMax * sizeOfDataType
        this.rowIndices = rowIndices;

    }

    @Override
    public boolean isLogical() {
        return logical;
    }

    @Override
    public boolean isComplex() {
        return complex;
    }

    @Override
    public double getDouble(int index) {
        return orLogical(index < 0 ? getDefaultValue() : real.getDouble(index));
    }

    @Override
    public void setDouble(int index, double value) {
        throw new IllegalStateException("This sparse matrix can't be modified.");
    }

    @Override
    public double getImaginaryDouble(int index) {
        if (!complex) return 0;
        return orLogical(index < 0 ? getDefaultValue() : imaginary.getDouble(index));
    }

    @Override
    public void setImaginaryDouble(int index, double value) {
        throw new IllegalStateException("This sparse matrix can't be modified.");
    }

    /**
     * Replace standard index calculation with sparse lookup.
     * <p>
     * The data is stored in a Compressed Column (CC) sparse matrix
     * format. The column index (numCols + 1) array contains the
     * starting point in the row indices for each column.
     * <p>
     * The row indices are sorted, so we can do a binary search
     * within the range of rows
     *
     * @return value index or -1 for empty values
     */
    @Override
    protected int getColumnMajorIndex(int row, int col) {
        // get range of possible row indices
        int fromIndex = Casts.sint32(columnIndices.getLong(col));
        int toIndex = Casts.sint32(columnIndices.getLong(col + 1)); // exclusive
        return searchRowBinary(fromIndex, toIndex, row);
    }

    protected int searchRowLinear(int fromIndex, int toIndex, int row) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (rowIndices.getLong(i) == row)
                return i;
        }
        return -1;
    }

    protected int searchRowBinary(int fromIndex, int toIndex, int row) {
        // binary search for desired row (copied from Arrays::binarySearch0)
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = Casts.sint32(rowIndices.getLong(mid));

            if (midVal < row)
                low = mid + 1;
            else if (midVal > row)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -1;
    }

    @Override
    public int getNzMax() {
        return nzMax;
    }

    @Override
    public void forEach(SparseConsumer action) {
        int i0 = (int) columnIndices.getLong(0);
        for (int col = 0; col < getNumCols(); col++) {
            int i1 = (int) columnIndices.getLong(col + 1);

            for (int i = i0; i < i1; i++) {
                int row = (int) rowIndices.getLong(i);
                double imag = complex ? imaginary.getDouble(i) : 0;
                action.accept(row, col, real.getDouble(i), imag);
            }

            i0 = i1;
        }

    }

    @Override
    public void close() throws IOException {
        rowIndices.close();
        columnIndices.close();
        real.close();
        if (complex) imaginary.close();
    }

    private final NumberStore columnIndices;
    private final NumberStore rowIndices;
    private final NumberStore real;
    private final NumberStore imaginary;
    private final boolean logical;
    private final boolean complex;
    final int nzMax;

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + rowIndices.getMat5Size()
                + columnIndices.getMat5Size()
                + real.getMat5Size()
                + (complex ? imaginary.getMat5Size() : 0);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        rowIndices.writeMat5(sink);
        columnIndices.writeMat5(sink);
        real.writeMat5(sink);
        if (complex) imaginary.writeMat5(sink);
    }

    @Override
    protected int subHashCode() {
        return Objects.hash(nzMax, logical, complex,
                NumberStore.hashCodeForType(imaginary, logical, MatlabType.Double),
                NumberStore.hashCodeForType(real, logical, MatlabType.Double),
                NumberStore.hashCodeForType(rowIndices, logical, MatlabType.Int64),
                NumberStore.hashCodeForType(columnIndices, logical, MatlabType.Int64));
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        MatSparseCSC other = (MatSparseCSC) otherGuaranteedSameClass;
        // all the primitive fields have to match before we bother looking at the data
        return other.nzMax == nzMax && 
                other.logical == logical &&
                other.complex == complex &&
                NumberStore.equalForType(other.imaginary, imaginary, logical, MatlabType.Double) &&
                NumberStore.equalForType(other.real, real, logical, MatlabType.Double) &&
                NumberStore.equalForType(other.rowIndices, rowIndices, logical, MatlabType.Int64) &&
                NumberStore.equalForType(other.columnIndices, columnIndices, logical, MatlabType.Int64);
    }
}
