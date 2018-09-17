package us.hebi.matlab.io.mat.mat5test;

import org.junit.Test;
import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.mat.Mat5File;
import us.hebi.matlab.io.types.MatFile;
import us.hebi.matlab.io.types.Matrix;
import us.hebi.matlab.io.types.ObjectStruct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * Tests and files partially copied copied from MatFileRW authored by
 * Matthew Dawson (matthew@mjdsystems.ca) as well as Piotr Smolinski
 * (piotr.smolinski.77@gmail.com)
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 08 Sep 2018
 */
public class McosTest {

    @Test
    public void testParsingSimpleEmptyMCOS() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/simpleempty.mat");
        assertEquals(2, mat.size());
        assertNotNull(mat.getSubsystem());

        List<String> expectedNames = Collections.emptyList();
        assertEquals(expectedNames, mat.getObject("obj").getFieldNames());
        assertEquals("SimpleEmpty", mat.getObject("obj").getClassName());
    }

    @Test
    public void testParsingMultipleSimpleEmptyMCOS() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/simpleempty_multiple.mat");
        assertEquals(3, mat.size());
        assertNotNull(mat.getSubsystem());

        List<String> expectedNames = Collections.emptyList();
        assertEquals(expectedNames, mat.getObject("obj1").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj2").getFieldNames());

        assertEquals("SimpleEmpty", mat.getObject("obj1").getClassName());
        assertEquals("SimpleEmpty", mat.getObject("obj2").getClassName());
    }

    @Test
    public void testParsingSimpleSingleTextUnmodifiedMCOS() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/simplesingletext_unmodified.mat");
        assertEquals(2, mat.size());
        assertNotNull(mat.getSubsystem());

        List<String> expectedNames = Collections.singletonList("test_text");
        assertEquals(expectedNames, mat.getObject("obj").getFieldNames());
        assertEquals("SimpleSingleText", mat.getObject("obj").getClassName());
        assertEquals("Default text", mat.getObject("obj").getChar("test_text").getString());

    }

    @Test
    public void testParsingSimpleSingleTextMultipleMCOS() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/simplesingletext_multiple.mat");
        assertEquals(4, mat.size());
        assertNotNull(mat.getSubsystem());

        List<String> expectedNames = Collections.singletonList("test_text");
        assertEquals(expectedNames, mat.getObject("obj1").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj2").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj3").getFieldNames());

        assertEquals("SimpleSingleText", mat.getObject("obj1").getClassName());
        assertEquals("SimpleSingleText", mat.getObject("obj2").getClassName());
        assertEquals("SimpleSingleText", mat.getObject("obj3").getClassName());

        assertEquals("other text 1", mat.getObject("obj1").getChar("test_text").getString());
        assertEquals("Default text", mat.getObject("obj2").getChar("test_text").getString());
        assertEquals("other text 3", mat.getObject("obj3").getChar("test_text").getString());
    }

    @Test
    public void testParsingHandleSinglePropertyMultipleMCOS() throws IOException {
        MatFile mat = MatTestUtil.readMat("mcos/handlesingle_multiple.mat");

        List<String> expectedNames = Collections.singletonList("myelement");
        assertEquals(expectedNames, mat.getObject("obj1").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj2").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj3").getFieldNames());
        assertEquals(expectedNames, mat.getObject("obj4").getFieldNames());

        assertEquals("HandleSingle", mat.getObject("obj1").getClassName());
        assertEquals("HandleSingle", mat.getObject("obj2").getClassName());
        assertEquals("HandleSingle", mat.getObject("obj3").getClassName());
        assertEquals("HandleSingle", mat.getObject("obj4").getClassName());

        assertEquals(25, mat.getObject("obj1").getMatrix("myelement").getByte(0));
        assertEquals(25, mat.getObject("obj3").getMatrix("myelement").getByte(0));
        assertEquals("testing", mat.getObject("obj2").getChar("myelement").getString());
        assertEquals("testing", mat.getObject("obj4").getChar("myelement").getString());

    }

    @Test
    public void testMultipleMCOSInArray() throws Exception {
        Mat5File mat = MatTestUtil.readMat("mcos/simplesingletext_multiplearray.mat");
        assertEquals(2, mat.size());
        assertNotNull(mat.getSubsystem());

        ObjectStruct a = mat.getObject("a");

        assertEquals(Collections.singletonList("test_text"), a.getFieldNames());
        assertEquals("SimpleSingleText", a.getClassName());
        assertEquals("", a.getPackageName());
        assertEquals(2, a.getNumRows());
        assertEquals(2, a.getNumCols());

        assertEquals(1, a.getMatrix("test_text", 0, 0).getInt(0));
        assertEquals(2, a.getMatrix("test_text", 1, 0).getInt(0));
        assertEquals(3, a.getMatrix("test_text", 0, 1).getInt(0));
        assertEquals(4, a.getMatrix("test_text", 1, 1).getInt(0));

    }

    @Test
    public void testReadingTimeSeries() throws IOException {
        Mat5File mat = MatTestUtil.readMat("mcos/timeseries.mat");
        assertEquals(2, mat.size());
        assertNotNull(mat.getSubsystem());

        ObjectStruct test = mat.getStruct("s").getObject("test");
        assertEquals("", test.getPackageName());
        assertEquals("timeseries", test.getClassName());
        assertEquals(14, test.getFieldNames().size());

        // Level 1 Indirection
        assertEquals(5, test.getMatrix("Data_").getNumElements());
        assertEquals(5, test.getMatrix("Quality_").getNumElements());
        assertEquals(0, test.getMatrix("Time_").getNumElements());
        assertEquals(10, test.getMatrix("Version").getInt(0));

        // Level 2 Indirection
        assertEquals("datametadata", test.getObject("DataInfo").getClassName());
        assertEquals("qualmetadata", test.getObject("QualityInfo").getClassName());
        assertEquals("timemetadata", test.getObject("TimeInfo").getClassName());

        // Level 3 Indirection
        assertEquals("interpolation", test.getObject("DataInfo").getObject("Interpolation").getClassName());
        assertEquals("seconds", test.getObject("TimeInfo").getChar("Units").getString());
        assertEquals(5, test.getObject("TimeInfo").get("Time_").getNumElements());

    }

    @Test
    public void testHandleClassReferenceEquality() throws Exception {
        Mat5File mat = MatTestUtil.readMat("mcos/handles.mat");
        assertNotNull(mat.getSubsystem());

        // Check value
        assertEquals(5, mat.getObject("objA").getMatrix("myPropA").getLong(0));

        // Check objB references the same object
        assertSame(
                mat.getObject("objA").getMatrix("myPropA"),
                mat.getObject("objB").getObject("myObjA").getMatrix("myPropA"));

        // Check objC references the same property field
        assertSame(
                mat.getObject("objA").getMatrix("myPropA"),
                mat.getObject("objC").getObject("myPropA").getMatrix("myPropA"));

    }

    /*
     t =
         2×2 table
         x     y
         __    __
         10    y1
         20    y2
     */
    @Test
    public void testTableClass() throws Exception {
        Mat5File mat = MatTestUtil.readMat("mcos/table.mat");

        ObjectStruct table = mat.getObject("t");
        assertEquals("table", table.getClassName());
        assertEquals("", table.getPackageName());
        assertEquals(2, table.getMatrix("ndims").getLong(0));
        assertEquals(2, table.getMatrix("nrows").getLong(0));

        // 'X' column
        assertEquals("x", table.getCell("varnames").getChar(0).getString());
        assertEquals(10, table.getCell("data").getMatrix(0).getLong(0));
        assertEquals(20, table.getCell("data").getMatrix(0).getLong(1));

        // 'Y' Column
        assertEquals("y", table.getCell("varnames").getChar(1).getString());
        assertEquals("y1", table.getCell("data").getChar(1).getRow(0));
        assertEquals("y2", table.getCell("data").getChar(1).getRow(1));

    }

    @Test
    public void testDoubleQuoteString() throws Exception {
        Mat5File mat = MatTestUtil.readMat("mcos/string.mat");

        // Old-style 'this is a character string'
        assertEquals("this is a character string", mat.getChar("singleQuoteString").getString());

        // New-style "this is not a character string" (>2016b)
        ObjectStruct string = mat.getObject("doubleQuoteString");
        assertEquals("string", string.getClassName());
        assertEquals("", string.getPackageName());

        // There is only one contained field ("any"), which is a [1x13 UInt64] matrix. So the actual
        // data is unfortunately stored in yet another undocumented format :-(
        //
        // For this string the matrix look as follows:
        //
        // any.getLong(i):
        //    [0] = 1
        //    [1] = 2
        //    [2] = 1
        //    [3] = 1
        //    [4] = 30
        //    [5] = 32370073300107380
        //    [6] = 9007693182861344
        //    [7] = 9007697478221934
        //    [8] = 29273822781767777
        //    [9] = 27866439313653857
        //    [10] = 9007688887631988
        //    [11] = 29555362188492915
        //    [12] = 6750318
        //
        // It looks like 5 header values followed by the encoded data. Considering some of the other
        // undocumented formats, the first 4 values probably indicate the dimension and how to
        // interpret the data (e.g. encoding), and the fifth value is likely the number of characters.

        Matrix data = string.getMatrix("any");
        assertArrayEquals(Mat5.dims(1, 13), data.getDimensions());

        // Convert data section to raw bytes
        ByteBuffer buffer = ByteBuffer.allocate(data.getNumElements() * SIZEOF_LONG);
        buffer.order(mat.getByteOrder());
        for (int i = 5; i < data.getNumElements(); i++) {
            buffer.putLong(data.getLong(i));
        }
        buffer.flip();

        // In this case the data looks like it was encoded as UInt16 characters, which is
        // the non-unicode default in MAT5 files
        String expected = "this is not a character string";
        StringBuilder actual = new StringBuilder(expected.length());
        while (actual.length() < expected.length()) {
            actual.append((char) buffer.getShort());
        }
        assertEquals(expected, actual.toString());

    }


}
