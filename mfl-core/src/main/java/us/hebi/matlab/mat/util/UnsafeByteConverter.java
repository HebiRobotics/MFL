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

package us.hebi.matlab.mat.util;

import java.nio.ByteOrder;

import static us.hebi.matlab.mat.util.UnsafeAccess.*;

/**
 * @author Florian Enner
 * @since 28 Aug 2018
 */
class UnsafeByteConverter implements ByteConverter {

    @Override
    public short getShort(ByteOrder order, byte[] bytes, int offset) {
        short value = UNSAFE.getShort(bytes, BYTE_ARRAY_OFFSET + offset);
        if (order == NATIVE_ORDER)
            return value;
        return Short.reverseBytes(value);
    }

    @Override
    public int getInt(ByteOrder order, byte[] bytes, int offset) {
        int value = UNSAFE.getInt(bytes, BYTE_ARRAY_OFFSET + offset);
        if (order == NATIVE_ORDER)
            return value;
        return Integer.reverseBytes(value);
    }

    @Override
    public long getLong(ByteOrder order, byte[] bytes, int offset) {
        long value = UNSAFE.getLong(bytes, BYTE_ARRAY_OFFSET + offset);
        if (order == NATIVE_ORDER)
            return value;
        return Long.reverseBytes(value);
    }

    @Override
    public float getFloat(ByteOrder order, byte[] bytes, int offset) {
        if (order == NATIVE_ORDER) {
            return UNSAFE.getFloat(bytes, BYTE_ARRAY_OFFSET + offset);
        } else {
            int bits = UNSAFE.getInt(bytes, BYTE_ARRAY_OFFSET + offset);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
    }

    @Override
    public double getDouble(ByteOrder order, byte[] bytes, int offset) {
        if (order == NATIVE_ORDER) {
            return UNSAFE.getDouble(bytes, BYTE_ARRAY_OFFSET + offset);
        } else {
            long bits = UNSAFE.getLong(bytes, BYTE_ARRAY_OFFSET + offset);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
    }

    @Override
    public void putShort(short value, ByteOrder order, byte[] bytes, int offset) {
        if (order != NATIVE_ORDER)
            value = Short.reverseBytes(value);
        UNSAFE.putShort(bytes, BYTE_ARRAY_OFFSET + offset, value);
    }

    @Override
    public void putInt(int value, ByteOrder order, byte[] bytes, int offset) {
        if (order != NATIVE_ORDER)
            value = Integer.reverseBytes(value);
        UNSAFE.putInt(bytes, BYTE_ARRAY_OFFSET + offset, value);
    }

    @Override
    public void putLong(long value, ByteOrder order, byte[] bytes, int offset) {
        if (order != NATIVE_ORDER)
            value = Long.reverseBytes(value);
        UNSAFE.putLong(bytes, BYTE_ARRAY_OFFSET + offset, value);
    }

    @Override
    public void putFloat(float value, ByteOrder order, byte[] bytes, int offset) {
        if (order == NATIVE_ORDER) {
            UNSAFE.putFloat(bytes, BYTE_ARRAY_OFFSET + offset, value);
        } else {
            int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            UNSAFE.putInt(bytes, BYTE_ARRAY_OFFSET + offset, bits);
        }
    }

    @Override
    public void putDouble(double value, ByteOrder order, byte[] bytes, int offset) {
        if (order == NATIVE_ORDER) {
            UNSAFE.putDouble(bytes, BYTE_ARRAY_OFFSET + offset, value);
        } else {
            long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            UNSAFE.putLong(bytes, BYTE_ARRAY_OFFSET + offset, bits);
        }
    }

    UnsafeByteConverter() {
        UnsafeAccess.requireUnsafe();
    }

}
