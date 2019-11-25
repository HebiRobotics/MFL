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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.AbstractStruct;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static us.hebi.matlab.mat.format.Mat5.*;
import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Struct serialization as described in MAT File Format 2018 p1-31.
 *
 * @author Florian Enner
 * @since 29 Aug 2018
 */
class MatStruct extends AbstractStruct implements Mat5Serializable {

    MatStruct(int[] dims) {
        super(dims);
    }

    MatStruct(int[] dims, String[] names, Array[][] values) {
        super(dims);
        checkArgument(getNumDimensions() == 2, "Structures are limited to two dimensions");
        int numElements = getNumElements();
        for (int field = 0; field < names.length; field++) {
            for (int i = 0; i < numElements; i++) {
                set(names[field], i, values[field][i]);
            }
        }
    }

    @Override
    protected Array getEmptyValue() {
        return Mat5.EMPTY_MATRIX;
    }

    protected String getClassName() {
        return "";
    }

    protected int getLongestFieldName() {
        int length = 0;
        for (String name : getFieldNames()) {
            length = Math.max(length, getLimitedNameLength(name));
        }
        return length + NULL_TERMINATOR_LENGTH;
    }

    @Override
    public int getMat5Size(String name) {
        final List<String> fieldNames = getFieldNames();
        final int numElements = getNumElements();
        final int numFields = fieldNames.size();

        // Common fields
        long size = MATRIX_TAG_SIZE;
        size += computeArrayHeaderSize(name, this);

        // Subfield -/4: Object only. Not struct
        if (getType() == MatlabType.Object) {
            String objectClassName = getClassName();
            size += Int8.computeSerializedSize(objectClassName.length());
        }

        // Subfield 4/5: Field Name Length
        size += Int32.computeSerializedSize(1);

        // Subfield 5/6: Field Names
        int numChars = getLongestFieldName() * numFields;
        size += Int8.computeSerializedSize(numChars); // ascii

        // Subfield 6/7: Fields
        for (int i = 0; i < numElements; i++) {
            for (int field = 0; field < numFields; field++) {
                size += computeArraySize(get(fieldNames.get(field), i));
            }
        }

        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Struct contents are larger than the 2GB variable limit of MAT5 files.");
        }
        return (int) size;
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        final List<String> fieldNames = getFieldNames();
        final int numElements = getNumElements();
        final int numFields = fieldNames.size();

        // Common fields
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);

        // Subfield -/4: Object only. Not struct
        if (getType() == MatlabType.Object) {
            String objectClassName = getClassName();
            Int8.writeBytesWithTag(objectClassName.getBytes(Charsets.US_ASCII), sink);
        }

        // Subfield 4/5: Field Name Length
        int longestName = getLongestFieldName();
        int numChars = longestName * numFields;
        Int32.writeIntsWithTag(new int[]{longestName}, sink);

        // Subfield 5/6: Field Names
        byte[] ascii = new byte[numChars];
        Arrays.fill(ascii, NULL_TERMINATOR);
        for (int i = 0; i < numFields; i++) {
            String fieldName = getLimitedName(getFieldNames().get(i));
            byte[] bytes = fieldName.getBytes(Charsets.US_ASCII);
            System.arraycopy(bytes, 0, ascii, i * longestName, bytes.length);
        }
        Int8.writeBytesWithTag(ascii, sink);

        // Subfield 6/7: Fields
        checkArgument(getNumDimensions() == 2, "Structures are limited to two dimensions");
        for (int i = 0; i < numElements; i++) {
            for (int field = 0; field < numFields; field++) {
                writeNestedArray(get(fieldNames.get(field), i), sink);
            }
        }

    }

    private static final byte NULL_TERMINATOR = (byte) '\0';
    private static final int NULL_TERMINATOR_LENGTH = 1;

}
