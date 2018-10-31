package us.hebi.matlab.mat.format.mat5test;

import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.format.Mat5Subsystem;
import us.hebi.matlab.mat.types.Char;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
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
