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

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Represents a root element, i.e., an array with a variable name.
 * <p>
 * Note that the MAT5 format technically allows names for nested arrays,
 * but in reality this is only useful for the root level. All nested arrays
 * should always have an empty name as structs store the name of each field,
 * and cell arrays don't use names at all.
 *
 * @author Florian Enner
 * @since 8 May 2018
 */
public final class NamedArray {

    public NamedArray(String name, Array value) {
        this.name = checkNotNull(name, "Name can't be null");
        this.value = checkNotNull(value, "Value can't be null");
    }

    public String getName() {
        return name;
    }

    public Array getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + " = " + value;
    }

    private final String name;
    private final Array value;

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof NamedArray) {
            NamedArray other = (NamedArray) obj;
            return other.name.equals(name) && other.value.equals(value);
        } else {
            return false;
        }
    }
}
