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

/**
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractCharBase extends AbstractArray implements Char {

    protected AbstractCharBase(int[] dims) {
        super(dims);
    }

    // ---- To be implemented

    @Override
    public abstract CharSequence asCharSequence();

    @Override
    public abstract char getChar(int index);

    @Override
    public abstract void setChar(int index, char value);

    // ---- Convenience accessors

    @Override
    public MatlabType getType() {
        return MatlabType.Character;
    }

    @Override
    public String getString() {
        if (getNumRows() != 1)
            throw new IllegalStateException("Char is not a single row char string");
        return getRow(0);
    }

    @Override
    public String getRow(int row) {
        checkNumDimensions(2);
        int numCols = getNumCols();

        // thread local might be too large and use is likely single threaded
        synchronized (builder) {
            // Make sure there is enough space
            builder.ensureCapacity(numCols);
            builder.setLength(0);

            // Read until end of string character
            for (int col = 0; col < numCols; col++) {
                char c = getChar(row, col);
                if (c == '\0')
                    break;
                builder.append(c);
            }
            return builder.toString();
        }
    }

    @Override
    public char getChar(int row, int col) {
        return getChar(getColumnMajorIndex(row, col));
    }

    @Override
    public char getChar(int[] indices) {
        return getChar(getColumnMajorIndex(indices));
    }

    @Override
    public void setChar(int row, int col, char value) {
        setChar(getColumnMajorIndex(row, col), value);
    }

    @Override
    public void setChar(int[] indices, char value) {
        setChar(getColumnMajorIndex(indices), value);
    }

    protected final StringBuilder builder = new StringBuilder();

}
