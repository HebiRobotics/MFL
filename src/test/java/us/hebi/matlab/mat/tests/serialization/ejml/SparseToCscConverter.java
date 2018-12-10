package us.hebi.matlab.mat.tests.serialization.ejml;

import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.FMatrixSparseCSC;
import us.hebi.matlab.mat.types.Sparse;

/**
 * @author Florian Enner
 * @since 10 Dec 2018
 */
abstract class SparseToCscConverter implements Sparse.SparseConsumer {

    static class SparseToDCscConverter extends SparseToCscConverter {

        DMatrixSparseCSC convertToCSC(Sparse input, DMatrixSparseCSC output) {
            output.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());
            initializeConversion(output.col_idx, output.nz_rows, output.getNumCols());
            nz_values = output.nz_values;
            input.forEach(this);
            nz_values = null;
            finishConversion();
            output.indicesSorted = true;
            return output;
        }

        @Override
        public void accept(int row, int col, double real, double imaginary) {
            nz_values[valueIx] = real;
            super.accept(row, col, real, imaginary);
        }

        double[] nz_values;

    }

    static class SparseToFCscConverter extends SparseToCscConverter {

        FMatrixSparseCSC convertToCSC(Sparse input, FMatrixSparseCSC output) {
            output.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());
            initializeConversion(output.col_idx, output.nz_rows, output.getNumCols());
            nz_values = output.nz_values;
            input.forEach(this);
            nz_values = null;
            finishConversion();
            output.indicesSorted = true;
            return output;
        }

        @Override
        public void accept(int row, int col, double real, double imaginary) {
            nz_values[valueIx] = (float) real;
            super.accept(row, col, real, imaginary);
        }

        float[] nz_values;

    }

    void initializeConversion(int[] col_idx, int[] nz_rows, int numCols) {
        this.col_idx = col_idx;
        this.nz_rows = nz_rows;
        this.numCols = numCols;
        valueIx = 0;
        lastColIx = 0;
    }

    void finishConversion() {
        setEmptyColumnsUntil(numCols);
        this.col_idx = null;
        this.nz_rows = null;
    }

    @Override
    public void accept(int row, int col, double real, double imaginary) {
        // set value index
        nz_rows[valueIx] = row;

        // update indices of any empty columns
        setEmptyColumnsUntil(col);

        // set start index of next column to same as end index of current column
        col_idx[col + 1] = valueIx + 1;
        valueIx++;
        lastColIx = col;
    }

    private void setEmptyColumnsUntil(int col) {
        while (lastColIx < col) {
            col_idx[lastColIx + 1] = valueIx;
            lastColIx++;
        }
    }

    int[] col_idx;
    int[] nz_rows;
    int numCols;
    int valueIx;
    int lastColIx;

}
