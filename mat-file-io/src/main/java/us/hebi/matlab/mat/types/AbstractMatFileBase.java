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
 * Forwards convenience overloads to avoid cluttering the implementation
 *
 * @author Florian Enner
 * @since 14 Sep 2018
 */
public abstract class AbstractMatFileBase implements MatFile {

    @Override
    public Matrix getMatrix(String name) {
        return getArray(name);
    }

    @Override
    public Sparse getSparse(String name) {
        return getArray(name);
    }

    @Override
    public Char getChar(String name) {
        return getArray(name);
    }

    @Override
    public Struct getStruct(String name) {
        return getArray(name);
    }

    @Override
    public ObjectStruct getObject(String name) {
        return getArray(name);
    }

    @Override
    public Cell getCell(String name) {
        return getArray(name);
    }

    @Override
    public Matrix getMatrix(int index) {
        return getArray(index);
    }

    @Override
    public Sparse getSparse(int index) {
        return getArray(index);
    }

    @Override
    public Char getChar(int index) {
        return getArray(index);
    }

    @Override
    public Struct getStruct(int index) {
        return getArray(index);
    }

    @Override
    public ObjectStruct getObject(int index) {
        return getArray(index);
    }

    @Override
    public Cell getCell(int index) {
        return getArray(index);
    }

}
