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

import static us.hebi.matlab.mat.util.Bytes.*;

/**
 * @author Florian Enner
 * @since 28 Aug 2018
 */
class ArrayBoundsCheck implements ByteConverter {

    @Override
    public short getShort(ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_SHORT);
        return impl.getShort(order, bytes, offset);
    }

    @Override
    public int getInt(ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_INT);
        return impl.getInt(order, bytes, offset);
    }

    @Override
    public long getLong(ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_LONG);
        return impl.getLong(order, bytes, offset);
    }

    @Override
    public float getFloat(ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_FLOAT);
        return impl.getFloat(order, bytes, offset);
    }

    @Override
    public double getDouble(ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_DOUBLE);
        return impl.getDouble(order, bytes, offset);
    }

    @Override
    public void putShort(short value, ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_SHORT);
        impl.putShort(value, order, bytes, offset);
    }

    @Override
    public void putInt(int value, ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_INT);
        impl.putInt(value, order, bytes, offset);
    }

    @Override
    public void putLong(long value, ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_LONG);
        impl.putLong(value, order, bytes, offset);
    }

    @Override
    public void putFloat(float value, ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_FLOAT);
        impl.putFloat(value, order, bytes, offset);
    }

    @Override
    public void putDouble(double value, ByteOrder order, byte[] bytes, int offset) {
        checkBounds(bytes, offset, SIZEOF_DOUBLE);
        impl.putDouble(value, order, bytes, offset);
    }

    private void checkBounds(byte[] bytes, int offset, int length) {
        if (bytes == null || offset < 0 || offset + length > bytes.length)
            throw new ArrayIndexOutOfBoundsException();
    }

    ArrayBoundsCheck(ByteConverter impl) {
        this.impl = impl;
    }

    final ByteConverter impl;

}
