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
 * Represents a numeric matrix of all types, e.g., [double], [single],
 * [uint16] etc.
 * Behavior:
 * - non-complex matrices always read zero for the imaginary part
 * - returned value gives the same result as reading the value and casting it in MATLAB, e.g., 'x = int8(value(index))'
 * - Java has no unsigned values, so there are only accessors for signed values. Naming follows Java convention
 * -- single = float
 * -- double = double
 * -- int8 = byte
 * -- int16 = short
 * -- etc.
 * - Reading unsigned values can be done using casts, e.g., short unsignedValue = Casts.uint8(matrix.getByte());
 * - index are longs as they may go beyond 2GB (although that will require support for Mat7.3)
 * <p>
 * Note that all MATLAB variables are really N-dimensional arrays, and that
 * 'matrix' only refers to two-dimensional arrays often used for linear algebra.
 * However, most users use numerical arrays in this way and are more familiar
 * with the term.
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Matrix extends Array {

    boolean isLogical();

    boolean isComplex();

    // ------- Index Accessors (real)

    boolean getBoolean(int index);

    byte getByte(int index);

    short getShort(int index);

    int getInt(int index);

    long getLong(int index);

    float getFloat(int index);

    double getDouble(int index);

    void setBoolean(int index, boolean value);

    void setByte(int index, byte value);

    void setShort(int index, short value);

    void setInt(int index, int value);

    void setLong(int index, long value);

    void setFloat(int index, float value);

    void setDouble(int index, double value);

    // ------- 2D Accessors (real)

    boolean getBoolean(int row, int col);

    byte getByte(int row, int col);

    short getShort(int row, int col);

    int getInt(int row, int col);

    long getLong(int row, int col);

    float getFloat(int row, int col);

    double getDouble(int row, int col);

    void setBoolean(int row, int col, boolean value);

    void setByte(int row, int col, byte value);

    void setShort(int row, int col, short value);

    void setInt(int row, int col, int value);

    void setLong(int row, int col, long value);

    void setFloat(int row, int col, float value);

    void setDouble(int row, int col, double value);

    // ------- N-dimensional Accessors (real)

    boolean getBoolean(int[] indices);

    byte getByte(int[] indices);

    short getShort(int[] indices);

    int getInt(int[] indices);

    long getLong(int[] indices);

    float getFloat(int[] indices);

    double getDouble(int[] indices);

    void setBoolean(int[] indices, boolean value);

    void setByte(int[] indices, byte value);

    void setShort(int[] indices, short value);

    void setInt(int[] indices, int value);

    void setLong(int[] indices, long value);

    void setFloat(int[] indices, float value);

    void setDouble(int[] indices, double value);

    // ------- Index Accessors (imaginary)

    byte getImaginaryByte(int index);

    short getImaginaryShort(int index);

    int getImaginaryInt(int index);

    long getImaginaryLong(int index);

    float getImaginaryFloat(int index);

    double getImaginaryDouble(int index);

    void setImaginaryByte(int index, byte value);

    void setImaginaryShort(int index, short value);

    void setImaginaryInt(int index, int value);

    void setImaginaryLong(int index, long value);

    void setImaginaryFloat(int index, float value);

    void setImaginaryDouble(int index, double value);

    // ------- 2D Accessors (imaginary)

    byte getImaginaryByte(int row, int col);

    short getImaginaryShort(int row, int col);

    int getImaginaryInt(int row, int col);

    long getImaginaryLong(int row, int col);

    float getImaginaryFloat(int row, int col);

    double getImaginaryDouble(int row, int col);

    void setImaginaryByte(int row, int col, byte value);

    void setImaginaryShort(int row, int col, short value);

    void setImaginaryInt(int row, int col, int value);

    void setImaginaryLong(int row, int col, long value);

    void setImaginaryFloat(int row, int col, float value);

    void setImaginaryDouble(int row, int col, double value);

    // ------- N-dimensional Accessors (imaginary)

    byte getImaginaryByte(int[] indices);

    short getImaginaryShort(int[] indices);

    int getImaginaryInt(int[] indices);

    long getImaginaryLong(int[] indices);

    float getImaginaryFloat(int[] indices);

    double getImaginaryDouble(int[] indices);

    void setImaginaryByte(int[] indices, byte value);

    void setImaginaryShort(int[] indices, short value);

    void setImaginaryInt(int[] indices, int value);

    void setImaginaryLong(int[] indices, long value);

    void setImaginaryFloat(int[] indices, float value);

    void setImaginaryDouble(int[] indices, double value);

}
