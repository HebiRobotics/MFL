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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Florian Enner
 * @since 14 Sep 2018
 */
public interface MatFile extends Closeable, Iterable<NamedArray> {

    Matrix getMatrix(String name);

    Sparse getSparse(String name);

    Char getChar(String name);

    Struct getStruct(String name);

    ObjectStruct getObject(String name);

    Cell getCell(String name);

    Matrix getMatrix(int index);

    Sparse getSparse(int index);

    Char getChar(int index);

    Struct getStruct(int index);

    ObjectStruct getObject(int index);

    Cell getCell(int index);

    // Unchecked casting to simplify casts to uncommon types
    <T extends Array> T getArray(String name);

    // Unchecked casting to simplify casts to uncommon types
    <T extends Array> T getArray(int index);

    MatFile addArray(String name, Array value);

    MatFile addArray(NamedArray entry);

    /**
     * @return number of contained arrays
     */
    int size();

    @Override
    Iterator<NamedArray> iterator();

    /**
     * Closes all contained arrays
     */
    @Override
    void close() throws IOException;

    /**
     * Computes the resulting file size if compression is disabled. Since
     * compression usually reduces the file size, this can be seen as a
     * maximum expected size.
     * <p>
     * This is useful to e.g. pre-allocate a buffer or file that can be
     * truncated once the actual file size is known.
     * <p>
     * Note that it is not guaranteed that compression will result in a
     * smaller file size, e.g., we have seen this happen on small arrays
     * with little data. Thus, for small arrays, you should add padding.
     *
     * @return serialized size in bytes including header
     */
    long getUncompressedSerializedSize();

    /**
     * Serializes this mat file including header and content to
     * the specified sink. The data may be compressed on the way.
     *
     * @return this
     */
    MatFile writeTo(Sink sink) throws IOException;

}
