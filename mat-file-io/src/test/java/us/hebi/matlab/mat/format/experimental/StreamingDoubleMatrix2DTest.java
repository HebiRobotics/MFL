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

package us.hebi.matlab.mat.format.experimental;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.util.Casts.*;

/**
 * @author Florian Enner
 * @since 08 May 2018
 */
public class StreamingDoubleMatrix2DTest {

    static File folder = new File("tmp-StreamingDoubleMatrix2DTest");
    static ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

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
        StreamingDoubleMatrix2D matrix = StreamingDoubleMatrix2D.createRowMajor(folder, "position", numCols);
        for (int i = 0; i < numCols * numRows; i++) {
            matrix.addValue(i);
        }
        assertArrayEquals(new int[]{numRows, numCols}, matrix.getDimensions());

        // Write data to memory
        try (Sink sink = Sinks.wrap(buffer).nativeOrder()) {

            Mat5.newMatFile() // create file structure
                    .addArray(matrix.getName(), matrix) // add content
                    .writeTo(sink) // write to memory
                    .close(); // dispose any native resources

        }

        // Read from memory
        buffer.flip();
        Matrix actual = Mat5.newReader(Sources.wrap(buffer)).readMat().getMatrix("position");
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

    @Test
    public void testWriteStreamingConcurrentToFile() throws Exception {

        // Create a few matrices with different dimensions
        MatFile matFile = Mat5.newMatFile();
        for (int i = 0; i < 7; i++) {
            int cols = i * 3;
            int rows = i * 5;
            StreamingDoubleMatrix2D matrix = StreamingDoubleMatrix2D.createRowMajor(folder, "matrix" + i, cols);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    matrix.addValue(row * col);
                }
            }
            matFile.addArray(matrix.getName(), matrix);
        }

        // Create temporary file
        File file = new File(getClass().getName() + ".mat");
        assertTrue("Couldn't delete temporary file", !file.exists() || file.delete());

        // Write to disk
        try (MatFile mat = matFile;
             Sink sink = Sinks.newMappedFile(file, sint32(matFile.getUncompressedSerializedSize()))) {
            Mat5.newWriter(sink.nativeOrder())
                    .enableConcurrentCompression(Executors.newCachedThreadPool())
                    .setDeflateLevel(Deflater.BEST_SPEED)
                    .writeMat(mat);
        }

        assertTrue("Output file does not exist", file.exists());
        assertTrue("Output file is empty", file.length() > 0);

        // Read result
        try (Source source = Sources.openFile(file)) {
            Mat5File result = Mat5.newReader(source).readMat();
            result.close();
        }

        assertTrue("Couldn't delete temporary file", !file.exists() || file.delete());

    }

}
