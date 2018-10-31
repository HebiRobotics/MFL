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
 * Converts bytes inside a raw byte array to appropriate primitive values.
 *
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public interface ByteConverter {

    short getShort(ByteOrder order, byte[] bytes, int offset);

    int getInt(ByteOrder order, byte[] bytes, int offset);

    long getLong(ByteOrder order, byte[] bytes, int offset);

    float getFloat(ByteOrder order, byte[] bytes, int offset);

    double getDouble(ByteOrder order, byte[] bytes, int offset);

    void putShort(short value, ByteOrder order, byte[] bytes, int offset);

    void putInt(int value, ByteOrder order, byte[] bytes, int offset);

    void putLong(long value, ByteOrder order, byte[] bytes, int offset);

    void putFloat(float value, ByteOrder order, byte[] bytes, int offset);

    void putDouble(double value, ByteOrder order, byte[] bytes, int offset);

}
