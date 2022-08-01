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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.util.Bytes;
import us.hebi.matlab.mat.util.Casts;

import java.io.IOException;
import java.nio.ByteBuffer;

import static us.hebi.matlab.mat.util.Casts.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * MAT files can store numerical data in a compressed format, i.e.,
 * if the values for a double matrix are all small, it may decide
 * to write the data as a smaller type such as uint8. This class is
 * a wrapper around ByteBuffer that helps with type conversion.
 * <p>
 * TODO: Expand storage if possible?
 * Note: Reading a double matrix with uint8 data will currently
 * throw an exception if a user tries to load the modified matrix
 * with a (valid) value that is outside of the internal range.
 * We may want this to automatically expand the range to double.
 * In order to add this, we need to add
 * - knowledge of the maximum expansion type (parent class)
 * - a way for users to allocate the larger buffer
 * This hasn't been added yet because it's not clear whether
 * reading + modifying + saving MAT files is a common use case.
 *
 * @author Florian Enner
 * @since 03 May 2018
 */
public class UniversalNumberStore implements NumberStore {

    /**
     * The buffer will go into the life cycle of this store (i.e. close()),
     * so the reference should not be used outside.
     *
     * @param type
     * @param buffer
     */
    UniversalNumberStore(Mat5Type type, ByteBuffer buffer, BufferAllocator bufferAllocator) {
        this.type = type;
        this.buffer = checkNotNull(buffer);
        this.bufferAllocator = checkNotNull(bufferAllocator);
        this.numElements = buffer.remaining() / type.bytes();
    }

    @Override
    public int getNumElements() {
        return numElements;
    }

    @Override
    public double getDouble(int index) {
        switch (type) {
            case Single:
                return buffer.getFloat(getOffset(index));
            case Double:
                return buffer.getDouble(getOffset(index));
            default:
                return getLong(index);
        }
    }

    @Override
    public long getLong(int index) {
        switch (type) {
            case Int8:
                return buffer.get(getOffset(index));
            case Int16:
                return buffer.getShort(getOffset(index));
            case Int32:
                return buffer.getInt(getOffset(index));
            case Int64:
                return buffer.getLong(getOffset(index));
            case UInt8:
                return uint8(buffer.get(getOffset(index)));
            case UInt16:
                return uint16(buffer.getShort(getOffset(index)));
            case UInt32:
                return uint32(buffer.getInt(getOffset(index)));
            case UInt64:
                return buffer.getLong(getOffset(index));
            case Single:
                return (long) buffer.getFloat(getOffset(index));
            case Double:
                return (long) buffer.getDouble(getOffset(index));
            default:
                throw new IllegalArgumentException("Not a numerical type " + type);
        }
    }

    @Override
    public void setDouble(int index, double value) {
        switch (type) {
            case Single:
                buffer.putFloat(getOffset(index), (float) value);
                break;
            case Double:
                buffer.putDouble(getOffset(index), value);
                break;
            default:
                checkInputRange(Casts.isInteger(value), value);
                setLong(index, (long) value);
                break;
        }
    }

    @Override
    public void setLong(int index, long value) {
        switch (type) {
            case Int8:
            case UInt8:
                checkInputRange(Casts.fitsByte(value), value);
                buffer.put(getOffset(index), (byte) value);
                break;
            case Int16:
            case UInt16:
                checkInputRange(Casts.fitsShort(value), value);
                buffer.putShort(getOffset(index), (short) value);
                break;
            case Int32:
            case UInt32:
                checkInputRange(Casts.fitsInt(value), value);
                buffer.putInt(getOffset(index), (int) value);
                break;

            case Int64:
            case UInt64:
                buffer.putLong(getOffset(index), value);
                break;
            case Single:
                buffer.putFloat(getOffset(index), (float) value);
                break;
            case Double:
                buffer.putDouble(getOffset(index), (double) value);
                break;
            default:
                throw new IllegalArgumentException("Not a numerical type " + type);
        }
    }

    private void checkInputRange(boolean state, double value) {
        if (!state) {
            String format = "Internal store type '%s' can not hold input value %f";
            throw new IllegalArgumentException(String.format(format, type, value));
        }
    }

    private int getOffset(int index) {
        return index * type.bytes();
    }

    @Override
    public int getMat5Size() {
        return type.computeSerializedSize(numElements);
    }

    @Override
    public void writeMat5(Sink sink) throws IOException {
        // Switch endian-ness if necessary. Alternatively, we
        // could write individual elements into the output, but
        // this way we guarantee that the switch happens fully
        // buffered. It's also likely that any subsequent writes
        // use the same output order.
        if (buffer.order() != sink.order()) {
            Bytes.reverseByteOrder(buffer, type.bytes());
        }

        // Copy the entire buffer at once
        type.writeByteBufferWithTag(buffer, sink);
        buffer.rewind();

    }

    ByteBuffer getByteBuffer() {
        buffer.rewind();
        return buffer.slice();
    }

    @Override
    public void close() {
        if (buffer == null) {
            System.err.println("already released!");
            return;
        }

        // Release buffer back to the allocator
        bufferAllocator.release(buffer);
        buffer = null;
        bufferAllocator = null;
    }

    final Mat5Type type;
    private final int numElements;
    ByteBuffer buffer;
    private BufferAllocator bufferAllocator;

    static int hashCodeForType(NumberStore store, boolean logical, MatlabType type) {
        if (store == null) {
            return 0;
        }
        int hash = 1;
        if (logical) {
            for (int i = 0; i < store.getNumElements(); ++i) {
                hash = 31 * hash + (Casts.logical(store.getDouble(i)) ? 1 : 0);
            }
        } else {
            if (type == MatlabType.Single || type == MatlabType.Double) {
                for (int i = 0; i < store.getNumElements(); ++i) {
                    hash = 31 * hash + Compat.hashDouble(store.getDouble(i));
                }
            } else {
                for (int i = 0; i < store.getNumElements(); ++i) {
                    hash = 31 * hash + Compat.hashLong(store.getLong(i));
                }
            }
        }
        return hash;
    }

    static boolean equalForType(NumberStore a, NumberStore b, boolean logical, MatlabType type) {
        if ((a == null) != (b == null)) {
            //  null mismatch is easy
            return false;
        } else if (a == null) {
            // they're both null
            return true;
        }
        if (a instanceof UniversalNumberStore && b instanceof UniversalNumberStore) {
            UniversalNumberStore aCast = (UniversalNumberStore) a;
            UniversalNumberStore bCast = (UniversalNumberStore) b;
            if (aCast.type == bCast.type) {
                // when their types are equal, then their binary content must be exactly the same (much faster)
                return aCast.buffer.equals(bCast.buffer);
            }
        }
        if (logical) {
            // compare floating-point types as doubles
            for (int i = 0; i < a.getNumElements(); ++i) {
                if (Casts.logical(a.getDouble(i)) != Casts.logical(b.getDouble(i))) {
                    return false;
                }
            }
        } else {
            // we need to compare without casting losses, so we need to know if these stores are being used
            // as floating points or integer types
            if (type == MatlabType.Single || type == MatlabType.Double) {
                // compare floating-point types as doubles
                for (int i = 0; i < a.getNumElements(); ++i) {
                    if (a.getDouble(i) != b.getDouble(i)) {
                        return false;
                    }
                }
            } else {
                // and integer types as ints
                for (int i = 0; i < a.getNumElements(); ++i) {
                    if (a.getLong(i) != b.getLong(i)) {
                        return false;
                    }
                }
            }
        }
        // same number of elements and numeric values, so they're equal
        return true;
    }
}
