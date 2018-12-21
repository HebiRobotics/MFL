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

package us.hebi.matlab.mat.ejml;

import org.ejml.data.*;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Sparse;

import java.util.Arrays;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * @author Florian Enner
 * @since 09 Dec 2018
 */
public class Mat5Ejml {

    /**
     * Creates a thin wrapper around {@link org.ejml.data.Matrix} that serializes
     * the contained data in a MAT-File format that is appropriate for the input
     * type. Wrapper Arrays do not allocate additional storage for storing data.
     * Wrapper arrays are also agnostic to the size, so the original matrices may
     * be reshaped as needed.
     *
     * @param matrix Input Matrix. Not modified.
     * @return Wrapper that handles serialization of the data.
     */
    public static Array asArray(org.ejml.data.Matrix matrix) {
        checkNotNull(matrix, "Input matrix can't be null");

        // Sparse CSC 64/32
        if (matrix instanceof DMatrixSparseCSC)
            return new DMatrixSparseCSCWrapper((DMatrixSparseCSC) matrix);
        if (matrix instanceof FMatrixSparseCSC)
            return new FMatrixSparseCSCWrapper((FMatrixSparseCSC) matrix);
        if (matrix instanceof MatrixSparse)
            throw new IllegalArgumentException("Unsupported Sparse Matrix Type: " + matrix.getClass().getSimpleName());

        // Dense Real 64/32
        if (matrix instanceof DMatrix)
            return new DMatrixWrapper((DMatrix) matrix);
        if (matrix instanceof FMatrix)
            return new FMatrixWrapper((FMatrix) matrix);

        // Dense Complex 64/32
        if (matrix instanceof ZMatrix)
            return new ZMatrixWrapper((ZMatrix) matrix);
        if (matrix instanceof CMatrix)
            return new CMatrixWrapper((CMatrix) matrix);

        // Logical / Boolean
        if (matrix instanceof BMatrixRMaj)
            return new BMatrixRMajWrapper((BMatrixRMaj) matrix);

        throw new IllegalArgumentException("Unsupported Dense Matrix Type: " + matrix.getClass().getSimpleName());

    }

    /**
     * Converts {@link Array} into {@link org.ejml.data.Matrix}. The output
     * matrix will be reshaped as needed.
     * <p>
     * Note that the there are no checks whether the output type is appropriate
     * for the data, e.g., a sparse complex double array may convert to a dense
     * float matrix without throwing an error.
     *
     * @param input  Input Array of Matrix type. Not modified.
     * @param output Output Matrix. Modified.
     * @param <T>    Desired output object.
     * @return Converted matrix
     */
    public static <T extends org.ejml.data.Matrix> T convert(Array input, T output) {
        checkNotNull(input, "Input array can't be null");
        checkNotNull(output, "Output matrix can't be null");
        checkArgument(input instanceof Matrix, "Input Array is not a Matrix type");
        final Matrix array = (Matrix) input;

        // Make sure dimensions match
        reshapeOutputSize(array, output);

        // Sparse MAT to Sparse 64/32
        if (array instanceof Sparse && output instanceof MatrixSparse) {

            // Convert to Sparse CSC 64/32
            if (output instanceof DMatrixSparseCSC)
                convertToDMatrixSparseCSC((Sparse) array, (DMatrixSparseCSC) output);
            else if (output instanceof FMatrixSparseCSC)
                convertToFMatrixSparseCSC((Sparse) array, (FMatrixSparseCSC) output);

                // Convert to Sparse Triplet 64/32
            else if (output instanceof DMatrixSparseTriplet)
                convertToDMatrixSparseTriplet((Sparse) array, (DMatrixSparseTriplet) output);
            else if (output instanceof FMatrixSparseTriplet)
                convertToFMatrixSparseTriplet((Sparse) array, (FMatrixSparseTriplet) output);
            else
                throw new IllegalArgumentException("Unsupported sparse output type: " + output.getClass());

        } else {

            // Sparse MAT to dense 64
            if (array instanceof Sparse && output instanceof DMatrixD1)
                convertToDMatrix((Sparse) array, (DMatrixD1) output);
            else if (array instanceof Sparse && output instanceof ZMatrixD1)
                convertToZMatrix((Sparse) array, (ZMatrixD1) output);

                // Dense MAT to dense 64
            else if (output instanceof DMatrix)
                convertToDMatrix(array, (DMatrix) output);
            else if (output instanceof ZMatrix)
                convertToZMatrix(array, (ZMatrix) output);

                // Sparse/Dense MAT to dense 32
            else if (output instanceof FMatrix)
                convertToFMatrix(array, (FMatrix) output);
            else if (output instanceof CMatrix)
                convertToCMatrix(array, (CMatrix) output);

                // Logical/Boolean
            else if (output instanceof BMatrixRMaj)
                convertToBMatrixRMaj(array, (BMatrixRMaj) output);

            else
                throw new IllegalArgumentException("Unsupported dense output type: " + output.getClass());

        }

        return output;

    }

    private static void reshapeOutputSize(Matrix input, org.ejml.data.Matrix output) {
        checkNotNull(input);
        checkNotNull(output);
        checkArgument(input.getNumDimensions() == 2, "EJML only supports 2D matrices");
        if (input.getNumRows() != output.getNumRows() || input.getNumCols() != output.getNumCols()) {

            if (input instanceof Sparse && output instanceof MatrixSparse) {
                ((MatrixSparse) output).reshape(input.getNumRows(), input.getNumCols(), ((Sparse) input).getNzMax());
            } else if (output instanceof ReshapeMatrix) {
                ((ReshapeMatrix) output).reshape(input.getNumRows(), input.getNumCols());
            } else {
                throw new IllegalArgumentException("Output matrix has incorrect size and can't be reshaped");
            }

        }
    }

    private static void convertToBMatrixRMaj(Matrix input, BMatrixRMaj output) {
        reshapeOutputSize(input, output);
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.unsafe_set(row, col, input.getBoolean(row, col));
            }
        }
    }

    private static void convertToFMatrix(Matrix input, FMatrix output) {
        reshapeOutputSize(input, output);
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.unsafe_set(row, col, input.getFloat(row, col));
            }
        }
    }

    private static void convertToDMatrix(Matrix input, DMatrix output) {
        reshapeOutputSize(input, output);
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.unsafe_set(row, col, input.getDouble(row, col));
            }
        }
    }

    private static void convertToCMatrix(Matrix input, CMatrix output) {
        reshapeOutputSize(input, output);
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.setReal(row, col, input.getFloat(row, col));
                output.setImag(row, col, input.getImaginaryFloat(row, col));
            }
        }
    }

    private static void convertToZMatrix(Matrix input, ZMatrix output) {
        reshapeOutputSize(input, output);
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.setReal(row, col, input.getDouble(row, col));
                output.setImag(row, col, input.getImaginaryDouble(row, col));
            }
        }
    }

    private static void convertToDMatrix(Sparse input, final DMatrixD1 output) {
        reshapeOutputSize(input, output);
        Arrays.fill(output.data, 0, output.getNumElements(), 0d);
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                output.unsafe_set(row, col, real);
            }
        });
    }

    private static void convertToZMatrix(Sparse input, final ZMatrixD1 output) {
        reshapeOutputSize(input, output);
        Arrays.fill(output.data, 0, output.getDataLength(), 0d);
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                output.setReal(row, col, real);
                output.setImag(row, col, imaginary);
            }
        });
    }

    private static void convertToFMatrixSparseTriplet(Sparse input, final FMatrixSparseTriplet output) {
        reshapeOutputSize(input, output);
        output.zero();
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                output.addItem(row, col, (float) real);
            }
        });
    }

    private static void convertToDMatrixSparseTriplet(Sparse input, final DMatrixSparseTriplet output) {
        reshapeOutputSize(input, output);
        output.zero();
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                output.addItem(row, col, real);
            }
        });
    }

    private static void convertToFMatrixSparseCSC(Sparse input, FMatrixSparseCSC output) {
        SparseToCscConverter.convertToFMatrixSparseCSC(input, output);
    }

    private static void convertToDMatrixSparseCSC(Sparse input, DMatrixSparseCSC output) {
        SparseToCscConverter.convertToDMatrixSparseCSC(input, output);
    }

}
