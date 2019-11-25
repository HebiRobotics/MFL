/*-
 * #%L
 * MAT File Library / Core
 * %%
 * Copyright (C) 2018 - 2019 HEBI Robotics
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

import org.junit.Test;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Struct;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 25 Nov 2019
 */
public class MatStructTest {

    /**
     * Tests for an issue that caused the index map to not be recomputed
     * after a field removal, so the indices were wrong and potentially
     * out of bounds.
     */
    @Test
    public void testFieldIndices() {
        Struct struct = Mat5.newStruct();

        Matrix var1 = Mat5.newMatrix(2, 3);
        Matrix var2 = Mat5.newComplex(1, 2);
        Matrix var3 = Mat5.newScalar(27);

        assertEquals(var1, struct.set("var1", var1).get("var1"));
        assertEquals(var2, struct.set("var2", var2).get("var2"));
        assertEquals(var3, struct.set("var3", var3).get("var3"));

        assertEquals(var2, struct.remove("var2")[0]);
        assertEquals(var1, struct.get("var1"));
        assertEquals(var3, struct.get("var3"));

        try {
            assertEquals(var2, struct.get("var2"));
            fail("Field should not exist anymore");
        } catch (IllegalArgumentException e) {
        }

        assertEquals(var2, struct.set("var2", var2).get("var2"));
        assertEquals(var1, struct.get("var1"));
        assertEquals(var3, struct.get("var3"));

    }

}
