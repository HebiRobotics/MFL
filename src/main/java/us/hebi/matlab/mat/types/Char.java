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
 * Represents MATLAB's 'char' type
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Char extends Array {

    // Entire buffer in column major order
    CharSequence asCharSequence();

    // Returns first row as string if numRows is 1
    String getString();

    // Row as column string for 2D arrays
    String getRow(int row);

    char getChar(int index);

    char getChar(int row, int col);

    char getChar(int[] indices);

    void setChar(int index, char value);

    void setChar(int row, int col, char value);

    void setChar(int[] indices, char value);

}
