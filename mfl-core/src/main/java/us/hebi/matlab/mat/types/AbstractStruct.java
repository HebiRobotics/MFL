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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Struct implementation for mapping fields and indices.
 * <p>
 * Note that we don't need to check indices as the array access already
 * takes care of that, i.e., throws an out of bounds exception.
 *
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractStruct extends AbstractStructBase {

    protected AbstractStruct(int[] dims) {
        super(dims);
    }

    @Override
    public List<String> getFieldNames() {
        return fields;
    }

    @Override
    public Array[] remove(String field) {
        Integer fieldIndex = indexMap.remove(checkNonEmpty(field));
        if (fieldIndex == null)
            throw new IllegalArgumentException("A field named '" + field + "' doesn't exist.");
        fields.remove((int) fieldIndex);
        return values.remove((int) fieldIndex);
    }

    @Override
    @SuppressWarnings("unchecked") // simplifies casting
    public <T extends Array> T get(String field, int index) {
        Integer fieldIndex = indexMap.get(checkNonEmpty(field));
        if (fieldIndex == null)
            throw new IllegalArgumentException("Reference to non-existent field '" + field + "'");
        return (T) values.get(fieldIndex)[index];
    }

    @Override
    public Struct set(String field, int index, Array value) {
        getOrInitValues(field)[index] = value;
        return this;
    }

    protected abstract Array getEmptyValue();

    protected Array[] getOrInitValues(String field) {
        // Field exists
        Integer fieldIndex = indexMap.get(checkNonEmpty(field));
        if (fieldIndex != null)
            return values.get(fieldIndex);

        // Field needs to be initialized (starts as empty)
        Array[] value = new Array[getNumElements()];
        Arrays.fill(value, getEmptyValue());

        // Add to map
        indexMap.put(field, values.size());
        fields.add(field);
        values.add(value);
        return value;
    }

    protected static String checkNonEmpty(String field) {
        if (field == null || field.isEmpty())
            throw new IllegalArgumentException("Field name can't be empty.");
        return field;
    }

    @Override
    public void close() throws IOException {
        for (Array[] value : values) {
            for (Array array : value) {
                array.close();
            }
        }
        indexMap.clear();
        fields.clear();
        values.clear();
    }

    private final HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
    private final List<String> fields = new ArrayList<String>();
    private final List<Array[]> values = new ArrayList<Array[]>();

    @Override
    protected int subHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + indexMap.hashCode();
        result = prime * result + fields.hashCode();
        for (Array[] valueArray : values) {
            result = prime * result + Arrays.hashCode(valueArray);
        }
        return result;
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        AbstractStruct other = (AbstractStruct) otherGuaranteedSameClass;
        if (other.indexMap.equals(indexMap) &&
                other.fields.equals(fields) &&
                other.values.size() == values.size()) {
            for (int i = 0; i < values.size(); ++i) {
                if (!Arrays.equals(other.values.get(i), values.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
