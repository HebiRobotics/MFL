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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Florian Enner
 * @since 27 Aug 2018
 */
public class Bytes {

    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;

    public static int nextPowerOfTwo(int value) {
        if (value == 0)
            return 1;

        int highestBit = Integer.highestOneBit(value);
        if (highestBit == value)
            return value;

        return highestBit << 1;
    }

    public static ByteOrder reverseByteOrder(ByteOrder order) {
        return order == ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public static void reverseByteOrder(ByteBuffer buffer, int bytesPerElement) {
        final int offset = buffer.position();
        final int limit = buffer.limit();

        switch (bytesPerElement) {
            case SIZEOF_BYTE:
                break;
            case SIZEOF_SHORT:
                for (int i = offset; i < limit; i += SIZEOF_SHORT)
                    buffer.putShort(i, Short.reverseBytes(buffer.getShort(i)));
                break;
            case SIZEOF_INT:
                for (int i = offset; i < limit; i += SIZEOF_INT)
                    buffer.putInt(i, Integer.reverseBytes(buffer.getInt(i)));
                break;
            case SIZEOF_LONG:
                for (int i = offset; i < limit; i += SIZEOF_LONG)
                    buffer.putLong(i, Long.reverseBytes(buffer.getLong(i)));
                break;
            default:
                throw new IllegalStateException("Unexpected number of bytes per element: " + bytesPerElement);
        }

        buffer.order(reverseByteOrder(buffer.order()));
    }

    /**
     * Returns index of first value occurence, or the specified default value if none were found
     */
    public static int findFirst(byte[] bytes, int offset, int length, byte value, int defaultValue) {
        for (int i = 0; i < length; i++) {
            if (bytes[offset + i] == value)
                return i;
        }
        return defaultValue;
    }

}
