package us.hebi.matlab.io.experimental;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.types.*;
import us.hebi.matlab.common.util.Silencer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;
import static us.hebi.matlab.io.experimental.StreamingDoubleMatrix2D.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 08 May 2018
 */
public class StreamingDoubleMatrix2DTest {

    static File folder = new File("tmp-StreamingDoubleMatrix2DTest");
    static ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
    static Sink sink = Sinks.wrap(buffer);

    @Before
    public void clear() {
        assertTrue("Could not create temporary folder: " + folder.getAbsolutePath(), folder.mkdir());
        buffer.clear();
    }

    @After
    public void delete() {
        assertTrue("Could not delete temporary folder: " + folder.getAbsolutePath(), folder.delete());
    }

    @Test
    public void testWriteIncrementalStreaming() throws Exception {
        int numRows = 12;
        int numCols = 8;
        StreamingDoubleMatrix2D matrix = createRowMajor(folder, "position", numCols);
        for (int i = 0; i < numCols * numRows; i++) {
            matrix.addValue(i);
        }
        assertArrayEquals(new int[]{numRows, numCols}, matrix.getDimensions());

        // Create file structure
        MatFile matFile = Mat5.newMatFile();
        matFile.addArray(matrix.getName(), matrix);

        // Write to memory
        sink.setByteOrder(ByteOrder.nativeOrder());
        matFile.writeTo(sink);

        // Dispose any native resources
        Silencer.close(matrix);

        // Read from memory
        buffer.flip();
        Matrix actual = Mat5.readFromBuffer(buffer).getMatrix("position");
        assertArrayEquals(new int[]{numRows, numCols}, actual.getDimensions());

        // Check data
        int expected = 0;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                assertEquals(expected, actual.getInt(row, col));
                expected++;
            }
        }

    }

}