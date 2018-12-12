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

package us.hebi.matlab.mat.tests.serialization.ejml;

import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.FMatrixSparseCSC;
import us.hebi.matlab.mat.types.Sparse;

import java.util.function.Supplier;

import static us.hebi.matlab.mat.tests.serialization.ejml.Mat5Ejml.*;

/**
 * @author Florian Enner
 * @since 10 Dec 2018
 */
abstract class SparseToCscConverter implements Sparse.SparseConsumer {

    static void convertToFMatrixSparseCSC(Sparse input, FMatrixSparseCSC output) {
        fCscConverter.get().convertToFSparseCSC(input, output);
    }

    static void convertToDMatrixSparseCSC(Sparse input, DMatrixSparseCSC output) {
        dCscConverter.get().convertToDSparseCSC(input, output);
    }

    private static final ThreadLocal<SparseToFCscConverter> fCscConverter = ThreadLocal.withInitial(new Supplier<SparseToFCscConverter>() {
        @Override
        public SparseToFCscConverter get() {
            return new SparseToFCscConverter();
        }
    });
    private static final ThreadLocal<SparseToDCscConverter> dCscConverter = ThreadLocal.withInitial(new Supplier<SparseToDCscConverter>() {
        @Override
        public SparseToDCscConverter get() {
            return new SparseToDCscConverter();
        }
    });

    static class SparseToFCscConverter extends SparseToCscConverter {

        void convertToFSparseCSC(Sparse input, FMatrixSparseCSC output) {
            output.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());
            initializeConversion(output.col_idx, output.nz_rows, output.nz_values, null);
            input.forEach(this);
            finishConversion(output.getNumCols());
            output.indicesSorted = true;
        }

        @Override
        protected void setValue(int nzIndex, double value) {
            fValues[nzIndex] = (float) value;
        }

    }

    static class SparseToDCscConverter extends SparseToCscConverter {

        void convertToDSparseCSC(Sparse input, DMatrixSparseCSC output) {
            output.reshape(input.getNumRows(), input.getNumCols(), input.getNzMax());
            initializeConversion(output.col_idx, output.nz_rows, null, output.nz_values);
            input.forEach(this);
            finishConversion(output.getNumCols());
            output.indicesSorted = true;
        }

        @Override
        protected void setValue(int nzIndex, double value) {
            dValues[nzIndex] = value;
        }

    }

    void initializeConversion(int[] col_idx, int[] nz_rows, float[] fValues, double[] dValues) {
        this.col_idx = col_idx;
        this.nz_rows = nz_rows;
        this.fValues = fValues;
        this.dValues = dValues;
        valueIx = 0;
        lastColIx = 0;
    }

    void finishConversion(int numCols) {
        setEmptyColumnsUntil(numCols);
        this.col_idx = null;
        this.nz_rows = null;
        this.fValues = null;
        this.dValues = null;
    }

    @Override
    public void accept(int row, int col, double real, double imaginary) {
        // set value index
        nz_rows[valueIx] = row;
        setValue(valueIx, real);

        // update indices of any empty columns
        setEmptyColumnsUntil(col);

        // set start index of next column to same as end index of current column
        col_idx[col + 1] = valueIx + 1;
        valueIx++;
        lastColIx = col;
    }

    protected abstract void setValue(int nzIndex, double value);

    private void setEmptyColumnsUntil(int col) {
        while (lastColIx < col) {
            col_idx[lastColIx + 1] = valueIx;
            lastColIx++;
        }
    }

    float[] fValues;
    double[] dValues;
    private int[] col_idx;
    private int[] nz_rows;
    private int valueIx;
    private int lastColIx;

}
