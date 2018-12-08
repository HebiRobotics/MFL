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
 * Forwards convenience methods to a minimum required set
 *
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractStructBase extends AbstractArray implements Struct {

    protected AbstractStructBase(int[] dims) {
        super(dims);
    }

    // ---- Actual Accessors

    @Override
    public abstract <T extends Array> T get(String field, int index);

    @Override
    public abstract Struct set(String field, int index, Array value);

    // ---- Convenience overloads

    @Override
    public MatlabType getType() {
        return MatlabType.Structure;
    }

    @Override
    public Matrix getMatrix(String field) {
        return get(field);
    }

    @Override
    public Sparse getSparse(String field) {
        return get(field);
    }

    @Override
    public Char getChar(String field) {
        return get(field);
    }

    @Override
    public Struct getStruct(String field) {
        return get(field);
    }

    @Override
    public ObjectStruct getObject(String field) {
        return get(field);
    }

    @Override
    public Cell getCell(String field) {
        return get(field);
    }

    @Override
    public <T extends Array> T get(String field) {
        if (getNumElements() != 1)
            throw new IllegalStateException("Scalar structure required for this request.");
        return get(field, 0);
    }

    @Override
    public Struct set(String field, Array value) {
        if (getNumElements() != 1)
            throw new IllegalStateException("Scalar structure required for this assignment.");
        return set(field, 0, value);
    }

    @Override
    public Matrix getMatrix(String field, int index) {
        return get(field, index);
    }

    @Override
    public Matrix getMatrix(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Matrix getMatrix(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public Sparse getSparse(String field, int index) {
        return get(field, index);
    }

    @Override
    public Sparse getSparse(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Sparse getSparse(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public Char getChar(String field, int index) {
        return get(field, index);
    }

    @Override
    public Char getChar(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Char getChar(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public Struct getStruct(String field, int index) {
        return get(field, index);
    }

    @Override
    public Struct getStruct(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Struct getStruct(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public ObjectStruct getObject(String field, int index) {
        return get(field, index);
    }

    @Override
    public ObjectStruct getObject(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public ObjectStruct getObject(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public Cell getCell(String field, int index) {
        return get(field, index);
    }

    @Override
    public Cell getCell(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Cell getCell(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public <T extends Array> T get(String field, int row, int col) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public <T extends Array> T get(String field, int[] indices) {
        return get(field, getColumnMajorIndex(indices));
    }

    @Override
    public Struct set(String field, int row, int col, Array value) {
        return get(field, getColumnMajorIndex(row, col));
    }

    @Override
    public Struct set(String field, int[] indices, Array value) {
        return get(field, getColumnMajorIndex(indices));
    }

}
