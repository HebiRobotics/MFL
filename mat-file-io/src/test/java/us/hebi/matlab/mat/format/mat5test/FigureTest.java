package us.hebi.matlab.mat.format.mat5test;

import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.ObjectStruct;
import us.hebi.matlab.mat.types.Struct;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 08 Sep 2018
 */
public class FigureTest {

    @Test
    public void testFigure() throws Exception {
        Mat5File matFile = MatTestUtil.readMat("figures/plot-simple.fig");

        // --- hgM_070000
        ObjectStruct format3Data = matFile.getStruct("hgM_070000")
                .getObject("GraphicsObjects")
                .getObject("Format3Data");

        assertEquals("Figure", format3Data.getClassName());
        assertEquals("manual", format3Data.getChar("NextPlotMode").getString());
        assertEquals("closereq", format3Data.getChar("CloseRequestFcn_IS").getString());
        assertArrayEquals(new int[]{16, 16}, format3Data.getMatrix("PointerShapeCData_IS").getDimensions());
        assertEquals(0, format3Data.getObject("matlab.graphics.internal.GraphicsCoreProperties.SerializableUIContextMenu").getNumElements());
        assertEquals("on", format3Data.getChar("matlab.graphics.internal.GraphicsCoreProperties.TopLevelSerializedObject").getString());

        // --- hgS_070000
        Struct properties = matFile.getStruct("hgS_070000")
                .getStruct("properties");

        assertEquals(-1919, properties.getMatrix("Position").getLong(0));
        assertEquals("auto", properties.getChar("PositionMode").getString());
        assertEquals("manual", properties.getChar("CurrentAxesMode").getString());
        assertEquals("add", properties.getChar("NextPlot").getString());
        assertArrayEquals(new int[]{1, 4}, properties.getMatrix("PaperPosition").getDimensions());
        assertEquals("auto", properties.getChar("PaperPositionMode").getString());
        assertEquals("manual", properties.getChar("ScreenPixelsPerInchMode").getString());

    }

}
