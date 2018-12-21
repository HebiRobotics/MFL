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

package us.hebi.matlab.mat.types;

/**
 * Forwards convenience overloads to a minimum required set
 *
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractCellBase extends AbstractArray implements Cell {

    protected AbstractCellBase(int[] dims) {
        super(dims);
    }

    // ---- Actual Accessors

    @Override
    public abstract <T extends Array> T get(int index);

    @Override
    public abstract Cell set(int index, Array value);

    // ---- Convenience overloads

    @Override
    public MatlabType getType() {
        return MatlabType.Cell;
    }

    @Override
    public Matrix getMatrix(int index) {
        return get(index);
    }

    @Override
    public Matrix getMatrix(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Matrix getMatrix(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Sparse getSparse(int index) {
        return get(index);
    }

    @Override
    public Sparse getSparse(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Sparse getSparse(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Char getChar(int index) {
        return get(index);
    }

    @Override
    public Char getChar(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Char getChar(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Struct getStruct(int index) {
        return get(index);
    }

    @Override
    public Struct getStruct(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Struct getStruct(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Cell getCell(int index) {
        return get(index);
    }

    @Override
    public Cell getCell(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Cell getCell(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public <T extends Array> T get(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public <T extends Array> T get(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Cell set(int row, int col, Array value) {
        set(getColumnMajorIndex(row, col), value);
        return this;
    }

    @Override
    public Cell set(int[] indices, Array value) {
        set(getColumnMajorIndex(indices), value);
        return this;
    }

}
