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

package us.hebi.matlab.common.memory;

import java.nio.ByteOrder;

/**
 * @author Florian Enner
 * @since 28 Aug 2018
 */
class NativeOrderConverter implements NativeAccess {

    @Override
    public byte getByte(Object object, long offset) {
        return impl.getByte(object, offset);
    }

    @Override
    public short getShort(ByteOrder order, Object object, long offset) {
        short value = impl.getShort(NATIVE_ORDER, object, offset);
        if (order == NATIVE_ORDER)
            return value;
        return Short.reverseBytes(value);
    }

    @Override
    public int getInt(ByteOrder order, Object object, long offset) {
        int value = impl.getInt(NATIVE_ORDER, object, offset);
        if (order == NATIVE_ORDER)
            return value;
        return Integer.reverseBytes(value);
    }

    @Override
    public long getLong(ByteOrder order, Object object, long offset) {
        long value = impl.getLong(NATIVE_ORDER, object, offset);
        if (order == NATIVE_ORDER)
            return value;
        return Long.reverseBytes(value);
    }

    @Override
    public float getFloat(ByteOrder order, Object object, long offset) {
        if (order == NATIVE_ORDER) {
            return impl.getFloat(NATIVE_ORDER, object, offset);
        } else {
            int bits = impl.getInt(NATIVE_ORDER, object, offset);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
    }

    @Override
    public double getDouble(ByteOrder order, Object object, long offset) {
        if (order == NATIVE_ORDER) {
            return impl.getDouble(NATIVE_ORDER, object, offset);
        } else {
            long bits = impl.getLong(NATIVE_ORDER, object, offset);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
    }

    @Override
    public void putByte(byte value, Object object, long offset) {
        impl.putByte(value, object, offset);
    }

    @Override
    public void putShort(short value, ByteOrder order, Object object, long offset) {
        if (order != NATIVE_ORDER)
            value = Short.reverseBytes(value);
        impl.putShort(value, NATIVE_ORDER, object, offset);
    }

    @Override
    public void putInt(int value, ByteOrder order, Object object, long offset) {
        if (order != NATIVE_ORDER)
            value = Integer.reverseBytes(value);
        impl.putInt(value, NATIVE_ORDER, object, offset);
    }

    @Override
    public void putLong(long value, ByteOrder order, Object object, long offset) {
        if (order != NATIVE_ORDER)
            value = Long.reverseBytes(value);
        impl.putLong(value, NATIVE_ORDER, object, offset);
    }

    @Override
    public void putFloat(float value, ByteOrder order, Object object, long offset) {
        if (order == NATIVE_ORDER) {
            impl.putFloat(value, NATIVE_ORDER, object, offset);
        } else {
            int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            impl.putInt(bits, NATIVE_ORDER, object, offset);
        }
    }

    @Override
    public void putDouble(double value, ByteOrder order, Object object, long offset) {
        if (order == NATIVE_ORDER) {
            impl.putDouble(value, NATIVE_ORDER, object, offset);
        } else {
            long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            impl.putLong(bits, NATIVE_ORDER, object, offset);
        }
    }

    NativeOrderConverter(NativeAccess impl) {
        this.impl = impl;
    }

    final NativeAccess impl;
    private final static ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

}
