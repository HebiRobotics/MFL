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

import java.io.Closeable;
import java.io.IOException;

import static us.hebi.matlab.mat.util.Preconditions.checkNotNull;

/**
 * @author Florian Enner
 * @since 14 Sep 2018
 */
public interface MatFile extends Closeable {

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

    MatFile addArray(String name, boolean isGlobal, Array value);

    MatFile addEntry(Entry entry);

    /**
     * @return number of arrays at the root level
     */
    int getNumEntries();

    /**
     * @return iterable of named arrays at the root level
     */
    Iterable<Entry> getEntries();

    /**
     * Clears the contained entries (without closing them)
     */
    void clear();

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

    /**
     * Represents a root element entry, i.e., a local or global variable that
     * has a name and an Array value.
     */
    final class Entry {

        public Entry(String name, boolean isGlobal, Array value) {
            this.name = checkNotNull(name, "Name can't be null");
            this.value = checkNotNull(value, "Value can't be null");
            this.isGlobal = isGlobal;
        }

        public String getName() {
            return name;
        }

        public Array getValue() {
            return value;
        }

        public boolean isGlobal() {
            return isGlobal;
        }

        @Override
        public String toString() {
            return name + (isGlobal() ? " (global)" : "") + " = " + value;
        }

        private final String name;
        private final Array value;
        private final boolean isGlobal;

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + value.hashCode() + (isGlobal ? 1231 : 1237);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Entry) {
                Entry other = (Entry) obj;
                return other.name.equals(name) && other.value.equals(value) && other.isGlobal == isGlobal;
            } else {
                return false;
            }
        }
    }

}
