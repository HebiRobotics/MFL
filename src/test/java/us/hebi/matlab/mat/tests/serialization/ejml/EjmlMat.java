package us.hebi.matlab.mat.tests.serialization.ejml;

import org.ejml.data.*;
import us.hebi.matlab.mat.tests.serialization.ejml.SparseToCscConverter.SparseToDCscConverter;
import us.hebi.matlab.mat.tests.serialization.ejml.SparseToCscConverter.SparseToFCscConverter;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Sparse;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * @author Florian Enner
 * @since 09 Dec 2018
 */
public class EjmlMat {

    public static Array asArray(org.ejml.data.Matrix matrix) {
        checkNotNull(matrix, "Input matrix can't be null");

        // Sparse CSC 64/32
        if (matrix instanceof DMatrixSparseCSC)
            return new DMatrixSparseCSCWrapper((DMatrixSparseCSC) matrix);
        else if (matrix instanceof FMatrixSparseCSC)
            return new FMatrixSparseCSCWrapper((FMatrixSparseCSC) matrix);
        else if (matrix instanceof MatrixSparse)
            throw new IllegalArgumentException("Unsupported Sparse Matrix Type: " + matrix.getClass().getSimpleName());

        // Dense Real 64/32
        if (matrix instanceof DMatrix)
            return new DMatrixWrapper((DMatrix) matrix);
        else if (matrix instanceof FMatrix)
            return new FMatrixWrapper((FMatrix) matrix);

        // Dense Complex 64/32
        if (matrix instanceof ZMatrix)
            return new ZMatrixWrapper((ZMatrix) matrix);
        else if (matrix instanceof CMatrix)
            return new CMatrixWrapper((CMatrix) matrix);

        throw new IllegalArgumentException("Unsupported Dense Matrix Type: " + matrix.getClass().getSimpleName());

    }

    public static <T extends org.ejml.data.Matrix> T fromArray(Array array, T output) {
        checkNotNull(array, "Input array can't be null");
        checkNotNull(output, "Output matrix can't be null");
        checkArgument(array instanceof Matrix, "Input array is not a Matrix type");
        checkArgument(array.getNumDimensions() == 2, "EJML only supports 2D matrices");
        Matrix input = (Matrix) array;

        if (array instanceof Sparse && output instanceof MatrixSparse) {

            // Sparse 64/32
            if (output instanceof DMatrixSparseCSC)
                convertToDMatrixSparseCSC((Sparse) input, (DMatrixSparseCSC) output);
            else if (output instanceof FMatrixSparseCSC)
                convertToFMatrixSparseCSC((Sparse) input, (FMatrixSparseCSC) output);

            else if (output instanceof DMatrixSparseTriplet)
                convertToDMatrixSparseTriplet((Sparse) input, (DMatrixSparseTriplet) output);
            else if (output instanceof FMatrixSparseTriplet)
                convertToFMatrixSparseTriplet((Sparse) input, (FMatrixSparseTriplet) output);
            else
                throw new IllegalArgumentException("Unsupported sparse output type: " + output.getClass());

        } else {

            // Dense Real 64/32
            if (output instanceof DMatrixRMaj)
                convertToDMatrixRMaj(input, (DMatrixRMaj) output);
            else if (output instanceof FMatrixRMaj)
                convertToFMatrixRMaj(input, (FMatrixRMaj) output);

                // Dense Complex 64/32
            else if (output instanceof ZMatrixRMaj)
                convertToZMatrixRMaj(input, (ZMatrixRMaj) output);
            else if (output instanceof CMatrixRMaj)
                convertToCMatrixRMaj(input, (CMatrixRMaj) output);
            else
                throw new IllegalArgumentException("Unsupported dense output type: " + output.getClass());

        }

        // Dense matrices
        if (output instanceof FMatrixRMaj) {
            convertToFMatrixRMaj(input, (FMatrixRMaj) output);
            return output;

        }

        return output;

    }

    public static FMatrixRMaj convertToFMatrixRMaj(Matrix input) {
        return convertToFMatrixRMaj(input, null);
    }

    public static FMatrixRMaj convertToFMatrixRMaj(Matrix input, FMatrixRMaj output) {
        checkArgument(input.getNumDimensions() == 2, "EJML only supports 2D matrices");
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();

        // Size output appropriately
        if (output == null) {
            output = new FMatrixRMaj(rows, cols);
        } else {
            output.reshape(rows, cols);
        }

        // Copy data
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.unsafe_set(row, col, input.getFloat(row, col));
            }
        }

        return output;

    }

    public static DMatrixRMaj convertToDMatrixRMaj(Matrix input) {
        return convertToDMatrixRMaj(input, null);
    }

    public static DMatrixRMaj convertToDMatrixRMaj(Matrix input, DMatrixRMaj output) {
        checkArgument(input.getNumDimensions() == 2, "EJML only supports 2D matrices");
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();

        // Size output appropriately
        if (output == null) {
            output = new DMatrixRMaj(rows, cols);
        } else {
            output.reshape(rows, cols);
        }

        // Copy data
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.unsafe_set(row, col, input.getDouble(row, col));
            }
        }

        return output;

    }

    public static CMatrixRMaj convertToCMatrixRMaj(Matrix input) {
        return convertToCMatrixRMaj(input, null);
    }

    public static CMatrixRMaj convertToCMatrixRMaj(Matrix input, CMatrixRMaj output) {
        checkArgument(input.getNumDimensions() == 2, "EJML only supports 2D matrices");
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();

        // Size output appropriately
        if (output == null) {
            output = new CMatrixRMaj(rows, cols);
        } else {
            output.reshape(rows, cols);
        }

        // Copy real data
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.setReal(row, col, input.getFloat(row, col));
            }
        }

        // Copy complex data
        if (input.isComplex()) {
            for (int col = 0; col < cols; col++) {
                for (int row = 0; row < rows; row++) {
                    output.setImag(row, col, input.getImaginaryFloat(row, col));
                }
            }
        }

        return output;
    }

    public static ZMatrixRMaj convertToZMatrixRMaj(Matrix input) {
        return convertToZMatrixRMaj(input, null);
    }

    public static ZMatrixRMaj convertToZMatrixRMaj(Matrix input, ZMatrixRMaj output) {
        checkArgument(input.getNumDimensions() == 2, "EJML only supports 2D matrices");
        final int rows = input.getNumRows();
        final int cols = input.getNumCols();

        // Size output appropriately
        if (output == null) {
            output = new ZMatrixRMaj(rows, cols);
        } else {
            output.reshape(rows, cols);
        }

        // Copy real data
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                output.setReal(row, col, input.getDouble(row, col));
            }
        }

        // Copy complex data
        if (input.isComplex()) {
            for (int col = 0; col < cols; col++) {
                for (int row = 0; row < rows; row++) {
                    output.setImag(row, col, input.getImaginaryDouble(row, col));
                }
            }
        }

        return output;
    }

    public static DMatrixSparseTriplet convertToDMatrixSparseTriplet(Sparse input) {
        return convertToDMatrixSparseTriplet(input, null);
    }

    public static DMatrixSparseTriplet convertToDMatrixSparseTriplet(Sparse input, DMatrixSparseTriplet output) {
        // Size output
        final DMatrixSparseTriplet out = output == null ? new DMatrixSparseTriplet() : output;
        out.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());

        // Copy set elements
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                out.addItem(row, col, real);
            }
        });
        return out;

    }

    public static DMatrixSparseCSC convertToDMatrixSparseCSC(Sparse input) {
        return convertToDMatrixSparseCSC(input, null);
    }

    public static DMatrixSparseCSC convertToDMatrixSparseCSC(Sparse input, DMatrixSparseCSC output) {
        if (output == null)
            output = new DMatrixSparseCSC(input.getNumRows(), input.getNumCols(), input.getNzMax());
        return dCscConverter.get().convertToCSC(input, output);
    }

    public static FMatrixSparseCSC convertToFMatrixSparseCSC(Sparse input) {
        return convertToFMatrixSparseCSC(input, null);
    }

    public static FMatrixSparseCSC convertToFMatrixSparseCSC(Sparse input, FMatrixSparseCSC output) {
        if (output == null)
            output = new FMatrixSparseCSC(input.getNumRows(), input.getNumCols(), input.getNzMax());
        return fCscConverter.get().convertToCSC(input, output);
    }

    private static final ThreadLocal<SparseToDCscConverter> dCscConverter = ThreadLocal.withInitial(SparseToDCscConverter::new);
    private static final ThreadLocal<SparseToFCscConverter> fCscConverter = ThreadLocal.withInitial(SparseToFCscConverter::new);

    public static FMatrixSparseTriplet convertToFMatrixSparseTriplet(Sparse input) {
        return convertToFMatrixSparseTriplet(input, null);
    }

    public static FMatrixSparseTriplet convertToFMatrixSparseTriplet(Sparse input, FMatrixSparseTriplet output) {
        // Size output
        final FMatrixSparseTriplet out = output == null ? new FMatrixSparseTriplet() : output;
        out.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());

        // Copy set elements
        input.forEach(new Sparse.SparseConsumer() {
            @Override
            public void accept(int row, int col, double real, double imaginary) {
                out.addItem(row, col, (float) real);
            }
        });
        return out;

    }


}
