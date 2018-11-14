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

package us.hebi.matlab.mat.tests.mat5;

import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.format.Mat5Subsystem;
import us.hebi.matlab.mat.types.Char;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 11 Sep 2018
 */
public class SubsystemTest {

    @Test
    public void testAddArray() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/handles.mat");

        // Test that adding a value keeps the subsystem at the end
        assertTrue(mat.getArray(3) instanceof Mat5Subsystem);
        mat.addArray("test", Mat5.newString("some value"));
        assertTrue(mat.getArray(3) instanceof Char);
        assertTrue(mat.getArray(4) instanceof Mat5Subsystem);
    }

}
