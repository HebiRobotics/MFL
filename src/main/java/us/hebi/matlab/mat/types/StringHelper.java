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

import us.hebi.matlab.mat.util.IndentingAppendable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utilities for creating useful toString() displays
 *
 * @author Florian Enner
 * @since 16 Sep 2018
 */
class StringHelper {

    static String toString(Collection<Variable> arrays) {
        StringBuilder builder = new StringBuilder();
        IndentingAppendable out = wrap(builder);

        // Find longest name
        int longest = 0;
        for (Variable variable : arrays) {
            longest = Math.max(longest, variable.getName().length());
        }

        // Add all Arrays
        boolean first = true;
        for (Variable array : arrays) {
            if (!first) out.append("\n");
            first = false;

            appendName(array.getName(), longest, out);
            out.append(" = ");
            append(array.getValue(), out);
        }

        return builder.toString();

    }

    static String toString(Array array) {
        StringBuilder builder = new StringBuilder();
        append(array, wrap(builder));
        return builder.toString();
    }

    private static void append(Array array, IndentingAppendable out) {
        // Special case empty arrays
        if (array.getNumElements() == 0) {
            if (array instanceof Cell) {
                out.append("{}");
                return;
            } else if (array instanceof Matrix) {
                out.append("[]");
                return;
            } else if (array instanceof Char) {
                out.append("''");
                return;
            }
        }

        // Special case char strings
        if (array instanceof Char) {
            Char c = (Char) array;
            if (c.getNumRows() == 1) {
                out.append("'").append(c.getString()).append("'");
                return;
            }
        }

        // Special case scalar matrices
        if (array instanceof Matrix) {
            Matrix matrix = (Matrix) array;
            if (matrix.getNumElements() == 1) {

                if (matrix.isLogical()) {
                    out.append(matrix.getBoolean(0));
                    return;
                }

                out.append(matrix.getDouble(0));
                if (matrix.isComplex()) {
                    out.append("+").append(matrix.getImaginaryDouble(0)).append("j");
                }
                return;
            }
        }

        // Add sub-fields to Structs & Objects
        if (array instanceof Struct) {

            // Class w/ dimensions
            String className = "struct";
            if (array instanceof ObjectStruct)
                className = getFullClassName((ObjectStruct) array);
            out.append(getDimString(array)).append(className);

            // Find longest field
            Struct struct = (Struct) array;
            int longestName = getLongestName(struct.getFieldNames());

            // List fields
            out.indent();
            for (String field : struct.getFieldNames()) {
                out.append("\n");
                appendName(field, longestName, out);

                // List values only if the struct is scalar
                if (struct.getNumElements() == 1) {
                    out.append(": ");
                    append(struct.get(field), out);
                }

            }
            out.unindent();
            return;
        }

        // Generic fallback
        appendGenericString(array, out);

    }

    private static void appendGenericString(Array array, IndentingAppendable out) {
        out.append(getDimString(array)).append(array.getType());
    }

    private static String getDimString(Array array) {
        return Arrays.toString(array.getDimensions())
                .replaceAll(", ", "x")
                .replace("[", "")
                .replace("]", " ");
    }

    private static int getLongestName(List<String> names) {
        int longest = 0;
        for (String name : names) {
            longest = Math.max(longest, name.length());
        }
        return longest;
    }

    private static void appendName(String name, int longest, IndentingAppendable out) {
        out.append(name.isEmpty() ? "\"\"" : name);
    }

    private static IndentingAppendable wrap(Appendable appendable) {
        IndentingAppendable out = new IndentingAppendable(appendable);
        out.setIndentString("    ");
        return out;
    }

    private static String getFullClassName(ObjectStruct object) {
        if (!object.getPackageName().isEmpty())
            return object.getPackageName() + "." + object.getClassName();
        return object.getClassName();
    }

}
