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

package us.hebi.matlab.mat.util;

import java.nio.ByteOrder;

import static java.nio.ByteOrder.*;

/**
 * @author Florian Enner
 * @since 26 Aug 2018
 */
class HeapByteConverter implements ByteConverter {

    public short getShort(ByteOrder order, byte[] bytes, int offset) {
        final int value;
        if (order == LITTLE_ENDIAN) {
            value = (bytes[offset + 0] & 0xFF) |
                    (bytes[offset + 1] & 0xFF) << 8;
        } else {
            value = (bytes[offset + 0] & 0xFF) << 8 |
                    (bytes[offset + 1] & 0xFF);
        }
        return (short) value;
    }

    public int getInt(ByteOrder order, byte[] bytes, int offset) {
        final int value;
        if (order == LITTLE_ENDIAN) {
            value = (bytes[offset + 0] & 0xFF) |
                    (bytes[offset + 1] & 0xFF) << 8 |
                    (bytes[offset + 2] & 0xFF) << 16 |
                    (bytes[offset + 3] & 0xFF) << 24;
        } else {
            value = (bytes[offset + 0] & 0xFF) << 24 |
                    (bytes[offset + 1] & 0xFF) << 16 |
                    (bytes[offset + 2] & 0xFF) << 8 |
                    (bytes[offset + 3] & 0xFF);
        }
        return value;
    }

    public long getLong(ByteOrder order, byte[] bytes, int offset) {
        final long value;
        if (order == LITTLE_ENDIAN) {
            value = ((long) bytes[offset + 0] & 0xFF) |
                    ((long) bytes[offset + 1] & 0xFF) << 8 |
                    ((long) bytes[offset + 2] & 0xFF) << 16 |
                    ((long) bytes[offset + 3] & 0xFF) << 24 |
                    ((long) bytes[offset + 4] & 0xFF) << 32 |
                    ((long) bytes[offset + 5] & 0xFF) << 40 |
                    ((long) bytes[offset + 6] & 0xFF) << 48 |
                    ((long) bytes[offset + 7] & 0xFF) << 56;
        } else {
            value = ((long) bytes[offset + 0] & 0xFF) << 56 |
                    ((long) bytes[offset + 1] & 0xFF) << 48 |
                    ((long) bytes[offset + 2] & 0xFF) << 40 |
                    ((long) bytes[offset + 3] & 0xFF) << 32 |
                    ((long) bytes[offset + 4] & 0xFF) << 24 |
                    ((long) bytes[offset + 5] & 0xFF) << 16 |
                    ((long) bytes[offset + 6] & 0xFF) << 8 |
                    ((long) bytes[offset + 7] & 0xFF);
        }
        return value;
    }

    public float getFloat(ByteOrder order, byte[] bytes, int offset) {
        return Float.intBitsToFloat(getInt(order, bytes, offset));
    }

    public double getDouble(ByteOrder order, byte[] bytes, int offset) {
        return Double.longBitsToDouble(getLong(order, bytes, offset));
    }

    @Override
    public void putShort(short value, ByteOrder order, byte[] bytes, int offset) {
        if (order == LITTLE_ENDIAN) {
            bytes[offset + 0] = (byte) (0xFF & value);
            bytes[offset + 1] = (byte) (0xFF & value >>> 8);
        } else {
            bytes[offset + 0] = (byte) (0xFF & value >>> 8);
            bytes[offset + 1] = (byte) (0xFF & value);
        }
    }

    @Override
    public void putInt(int value, ByteOrder order, byte[] bytes, int offset) {
        if (order == LITTLE_ENDIAN) {
            bytes[offset + 0] = (byte) (0xFF & value);
            bytes[offset + 1] = (byte) (0xFF & value >>> 8);
            bytes[offset + 2] = (byte) (0xFF & value >>> 16);
            bytes[offset + 3] = (byte) (0xFF & value >>> 24);
        } else {
            bytes[offset + 0] = (byte) (0xFF & value >>> 24);
            bytes[offset + 1] = (byte) (0xFF & value >>> 16);
            bytes[offset + 2] = (byte) (0xFF & value >>> 8);
            bytes[offset + 3] = (byte) (0xFF & value);
        }
    }

    @Override
    public void putLong(long value, ByteOrder order, byte[] bytes, int offset) {
        if (order == LITTLE_ENDIAN) {
            bytes[offset + 0] = (byte) (0xFF & value);
            bytes[offset + 1] = (byte) (0xFF & value >>> 8);
            bytes[offset + 2] = (byte) (0xFF & value >>> 16);
            bytes[offset + 3] = (byte) (0xFF & value >>> 24);
            bytes[offset + 4] = (byte) (0xFF & value >>> 32);
            bytes[offset + 5] = (byte) (0xFF & value >>> 40);
            bytes[offset + 6] = (byte) (0xFF & value >>> 48);
            bytes[offset + 7] = (byte) (0xFF & value >>> 56);
        } else {
            bytes[offset + 0] = (byte) (0xFF & value >>> 56);
            bytes[offset + 1] = (byte) (0xFF & value >>> 48);
            bytes[offset + 2] = (byte) (0xFF & value >>> 40);
            bytes[offset + 3] = (byte) (0xFF & value >>> 32);
            bytes[offset + 4] = (byte) (0xFF & value >>> 24);
            bytes[offset + 5] = (byte) (0xFF & value >>> 16);
            bytes[offset + 6] = (byte) (0xFF & value >>> 8);
            bytes[offset + 7] = (byte) (0xFF & value);
        }
    }

    @Override
    public void putFloat(float value, ByteOrder order, byte[] bytes, int offset) {
        putInt(Float.floatToRawIntBits(value), order, bytes, offset);
    }

    @Override
    public void putDouble(double value, ByteOrder order, byte[] bytes, int offset) {
        putLong(Double.doubleToRawLongBits(value), order, bytes, offset);
    }

}
