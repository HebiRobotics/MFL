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

import us.hebi.matlab.mat.util.Compat;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Represents a root element, i.e., an named variable with an array value.
 * <p>
 * Note that the MAT5 format technically allows names for nested arrays,
 * but in reality this is only useful for the root level. All nested arrays
 * should always have an empty name as structs store the name of each field,
 * and cell arrays don't use names at all. The same is true for the global
 * flag.
 *
 * @author Florian Enner
 * @since 8 May 2018
 */
public final class Variable {

    public Variable(String name, boolean global, Array value) {
        this.name = checkNotNull(name, "Name can't be null");
        this.value = checkNotNull(value, "Value can't be null");
        this.global = global;
    }

    public String getName() {
        return name;
    }

    public Array getValue() {
        return value;
    }

    public boolean isGlobal() {
        return global;
    }

    @Override
    public String toString() {
        return name + (isGlobal() ? " (global)" : "") + " = " + value;
    }

    private final String name;
    private final Array value;
    private final boolean global;

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + value.hashCode() + Compat.hashCode(global);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Variable) {
            Variable other = (Variable) obj;
            return other.name.equals(name) && other.value.equals(value) && other.global == global;
        } else {
            return false;
        }
    }
}
