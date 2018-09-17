package us.hebi.matlab.io.experimental;

import org.junit.Test;
import us.hebi.matlab.common.util.Casts;
import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.types.MatFile;
import us.hebi.matlab.io.types.Sinks;
import us.hebi.matlab.io.types.Sources;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static us.hebi.matlab.io.mat.Mat5.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 16 Sep 2018
 */
public class ConcurrentMat5WriterTest {

    @Test
    public void writeFile() throws IOException {
        MatFile mat = Mat5.newMatFile()
                .addArray("var1", Mat5.newScalar(12.9))
                .addArray("var2", Mat5.newCell(2, 3))
                .addArray("var3", Mat5.newString("this is a test"))
                .addArray("var4", Mat5.newMatrix(dims(9, 17, 28, 4)));

        ByteBuffer buffer = ByteBuffer.allocate(Casts.sint32(mat.getUncompressedSerializedSize()));
        ConcurrentMat5Writer writer = new ConcurrentMat5Writer(Sinks.wrap(buffer));
        writer.writeFile(mat);

        buffer.flip();
        MatFile result = Mat5.newReader(Sources.wrap(buffer)).readFile();

        assertEquals(12.9, result.getMatrix("var1").getDouble(0), 0);
        assertArrayEquals(new int[]{2, 3}, result.getCell("var2").getDimensions());
        assertEquals("this is a test", result.getChar("var3").getString());
        assertArrayEquals(new int[]{9, 17, 28, 4}, result.getMatrix("var4").getDimensions());

    }

}