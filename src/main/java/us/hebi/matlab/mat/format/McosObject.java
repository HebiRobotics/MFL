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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains object data that can be referenced. Each object only
 * represents a single instance, i.e., a reference array would
 * contain multiple such objects. The property map forms automatically
 * and in the same order as the properties were added.
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
class McosObject {

    McosObject(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    String getPackageName() {
        return packageName;
    }

    String getClassName() {
        return className;
    }

    List<String> getFieldNames() {
        return fieldNames;
    }

    void set(String name, Array value) {
        if (properties.put(name, value) == null) {
            fieldNames.add(name);
        }
    }

    Array get(String name) {
        return properties.get(name);
    }

    private final String packageName;
    private final String className;
    private final List<String> fieldNames = new ArrayList<String>(8);
    private final Map<String, Array> properties = new HashMap<String, Array>(16);

    final static McosObject EMPTY = new McosObject("", "") {
        @Override
        public void set(String name, Array value) {
            throw new IllegalStateException("Can't set empty reference.");
        }
    };

    @Override
    public String toString() {
        return "'" + getClassName() + "' class";
    }

    @Override
    public int hashCode() {
        return Compat.hash(packageName, className, fieldNames, properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof McosObject) {
            McosObject other = (McosObject) obj;
            return other.packageName.equals(packageName) &&
                    other.className.equals(className) &&
                    other.fieldNames.equals(fieldNames) &&
                    other.properties.equals(properties);
        } else {
            return false;
        }
    }
}
