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

import java.io.IOException;
import java.util.Arrays;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Cell array implementation.
 * <p>
 * Note that we don't need to check indices as the array access already
 * takes care of that, i.e., throws an out of bounds exception.
 *
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractCell extends AbstractCellBase {

    protected AbstractCell(int[] dims, boolean isGlobal, Array[] values) {
        super(dims, isGlobal);
        checkArgument(values.length == getNumElements(), "invalid length");
        this.contents = values;
    }

    @Override
    @SuppressWarnings("unchecked") // simplifies casting
    public <T extends Array> T get(int index) {
        return (T) contents[index];
    }

    @Override
    public Cell set(int index, Array value) {
        contents[index] = value;
        return this;
    }

    protected abstract Array getEmptyValue();

    @Override
    public void close() throws IOException {
        for (Array array : contents) {
            array.close();
        }
        Arrays.fill(contents, getEmptyValue());
    }

    protected final Array[] contents;

}
