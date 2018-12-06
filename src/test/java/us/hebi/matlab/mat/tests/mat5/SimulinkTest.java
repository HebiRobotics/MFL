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
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.ObjectStruct;
import us.hebi.matlab.mat.types.Struct;

import static org.junit.Assert.*;

/**
 * Tests copied from MatFileRW::SimulinkMatTest
 *
 * @author Florian Enner
 * @since 09 Sep 2018
 */
public class SimulinkTest {

    /**
     * Contains a single "Data" element that has no name. All other data is inside
     * subsystem.
     */
    @Test
    public void testParsingQuadraticRootsTET() throws Exception {
        // the object graph is not hierarchical or even a DAG.
        // it is a cyclic graph which causes StackOverflow on equality check
        // rare enough that it's not clear how much investigation is warranted...
        boolean equalityCheck = false;
        Mat5File mat = MatTestUtil.readMat("simulink/simulink_tet_out.mat", true, true, equalityCheck);

        // First check that the root element is correct.
        final ObjectStruct data = mat.getObject(0); // has no name
        assertEquals("", data.getPackageName());
        assertEquals("Data", data.getClassName());
        assertEquals(1, data.getNumElements());
        assertEquals(10, data.getFieldNames().size());
        assertEquals("quad_fcn_subtype", data.getChar("function_name").getString());
        assertEquals("c,a,b:{x:real|(a=0 => x /= 0) AND (a /= 0 => (x^2) - 4*a*c >= 0)}", data.getChar("function_inputs").getString());
        assertEquals(0, data.getMatrix("open").getLong(0));
        assertEquals(0, data.getMatrix("fig").getNumElements());
        assertTrue(data.getMatrix("multi_mode").getBoolean(0));
        assertFalse(data.getMatrix("checked").getBoolean(0));

        // Next, make sure the settings structure came out right.  Not super important, but a good test.
        final Struct settings = data.getStruct("settings");
        assertEquals(5, settings.getFieldNames().size());
        assertEquals(0, settings.getMatrix("inputs").getNumElements());
        assertEquals(1000, settings.getMatrix("count").getLong(0));
        assertEquals(100, settings.getMatrix("range").getLong(0));
        assertEquals(0, settings.getMatrix("except").getLong(0));

        // Next, verify Grid2, as it is easiest.
        final ObjectStruct grid2 = data.getObject("Grid2");
        assertEquals("Grid", grid2.getClassName());
        assertEquals(9, grid2.getFieldNames().size());
        assertEquals(0, grid2.get("split_pb").getNumElements());
        assertEquals(0, grid2.get("parent_grid").getNumElements());
        assertEquals(0, grid2.get("parent_cell").getNumElements());
        assertEquals(0, grid2.get("new_cell_pb").getNumElements());
        assertEquals(0, grid2.get("delete_cell_pb").getNumElements());

        assertEquals(2, grid2.getObject("cells").getNumElements());
        assertEquals("a == 0", grid2.getObject("cells").getChar("cond_text", 0).getString());
        assertEquals("a ~= 0", grid2.getObject("cells").getChar("cond_text", 1).getString());

    }

    /**
     * Data supplied by Github::br1egel (MatFileRW issue #18)
     */
    @Test
    public void testStatespaceObject() throws Exception {
        Mat5File mat = MatTestUtil.readMat("simulink/statespace.mat", false, false);
        assertEquals(19, mat
                .getObject("test_ss")
                .getMatrix("Version_")
                .getInt(0));
        assertEquals(0, mat
                .getObject("test_ss")
                .getObject("Data_")
                .getStruct("Delay")
                .getMatrix("Input")
                .getDouble(1), 0);
    }

}
