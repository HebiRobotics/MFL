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

import java.util.List;

/**
 * Represents MATLAB's struct array, e.g., 'x = struct(2,2)'
 * Behavior:
 * - Contains other Arrays referenced by a named field
 * - Elements in the array have the same fields
 * - Values of all elements are independent
 * - When setting a new field to a value, all other elements
 * in the array w/ field are initialized to empty
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Struct extends Array {

    List<String> getFieldNames();

    /**
     * Removes the specified field from this struct. Same behavior
     * as rmfield(struct,'name') in MATLAB.
     * <p>
     * Fields that get deleted don't have their associated values
     * closed. Users would need to close the returned values
     * manually.
     *
     * @param field field to be removed
     * @return arrays of previously associated values
     */
    Array[] remove(String field);

    // ------- Scalar Accessors (return first element)

    Matrix getMatrix(String field);

    Sparse getSparse(String field);

    Char getChar(String field);

    Struct getStruct(String field);

    ObjectStruct getObject(String field);

    Cell getCell(String field);

    <T extends Array> T get(String field);

    Struct set(String field, Array value);

    // ------- Array Accessors

    Matrix getMatrix(String field, int index);

    Matrix getMatrix(String field, int row, int col);

    Matrix getMatrix(String field, int[] indices);

    Sparse getSparse(String field, int index);

    Sparse getSparse(String field, int row, int col);

    Sparse getSparse(String field, int[] indices);

    Char getChar(String field, int index);

    Char getChar(String field, int row, int col);

    Char getChar(String field, int[] indices);

    Struct getStruct(String field, int index);

    Struct getStruct(String field, int row, int col);

    Struct getStruct(String field, int[] indices);

    ObjectStruct getObject(String field, int index);

    ObjectStruct getObject(String field, int row, int col);

    ObjectStruct getObject(String field, int[] indices);

    Cell getCell(String field, int index);

    Cell getCell(String field, int row, int col);

    Cell getCell(String field, int[] indices);

    <T extends Array> T get(String field, int index);

    <T extends Array> T get(String field, int row, int col);

    <T extends Array> T get(String field, int[] indices);

    Struct set(String field, int index, Array value);

    Struct set(String field, int row, int col, Array value);

    Struct set(String field, int[] indices, Array value);

}
