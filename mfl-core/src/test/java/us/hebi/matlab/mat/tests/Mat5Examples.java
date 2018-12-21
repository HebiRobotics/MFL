/*-
 * #%L
 * MAT File Library
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

package us.hebi.matlab.mat.tests;

import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

import static org.junit.Assert.*;
import static us.hebi.matlab.mat.util.Casts.*;

/**
 * API Examples for creating MAT 5 files
 *
 * @author Florian Enner
 * @since 07 May 2018
 */
public class Mat5Examples {

    @Test
    public void testMatrixWriteRead() throws IOException {

        // Create arbitrary data
        Matrix eye3 = Mat5.newMatrix(3, 3, MatlabType.Int64);
        for (int i = 0; i < 3; i++) {
            eye3.setLong(i, i, i + 1);
        }

        // Attach to a file structure
        MatFile mat = Mat5.newMatFile();
        mat.addArray("identityMatrix", eye3);

        // Create buffer for storage
        int bufferSize = sint32(mat.getUncompressedSerializedSize());
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.order(ByteOrder.nativeOrder());

        // Write to buffer
        mat.writeTo(Sinks.wrap(buffer));

        // Read from buffer
        buffer.flip();
        MatFile result = Mat5.newReader(Sources.wrap(buffer)).readMat();

        // Access data
        assertEquals(2, result.getMatrix("identityMatrix").getLong(1, 1));

    }

    @Test
    public void testCreateAndWriteMatFile() throws IOException {
        // Build file structure
        MatFile matFile = Mat5.newMatFile()
                .addArray("var1", Mat5.newString("Test"))
                .addArray("var2", Mat5.newScalar(7))
                .addArray("var3", Mat5.newMatrix(8, 8))
                .addArray("var4", Mat5.newChar(7, 7));

        // Calculate max expected size assuming that compression will
        // reduce the result so we can pre-allocate a buffer.
        int maxExpectedSize = sint32(matFile.getUncompressedSerializedSize());
        ByteBuffer buffer = ByteBuffer.allocate(maxExpectedSize);
        try (Sink sink = Sinks.wrap(buffer)) {

            // Write file w/ header and content
            matFile.writeTo(sink);

        }
    }

    /**
     * All types have convenience overloads for 1D and 2D access
     */
    @Test
    public void testDoubleMatrix_2D() {
        Matrix matrix = Mat5.newMatrix(400, 2);

        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                double value = col * 23 + row * 11;
                matrix.setDouble(row, col, value);
                assertEquals(value, matrix.getDouble(row, col), DELTA);
            }
        }

    }

    /**
     * Multi-dimensional matrices are indexed via an index array (int[]). The
     * dims() and index() methods help with generating the int[] while also
     * improving readability.
     */
    @Test
    public void testDoubleMatrix_4D() {
        long value = 91219;

        Matrix matrix = Mat5.newMatrix(Mat5.dims(10, 11, 12, 13));

        int[] index = Mat5.index(9, 9, 9, 9);
        matrix.setLong(index, value);
        assertEquals(value, matrix.getLong(index));
    }

    /**
     * Independent of which setters are called, values are interpreted using the
     * internal type. If the value doesn't fit into the specified storage type,
     * it will throw an error.
     */
    @Test
    public void testUInt8Matrix() {
        Matrix matrix = Mat5.newMatrix(1, 1, MatlabType.UInt8);

        // valid uint8 value without signed bit
        matrix.setInt(0, 0x01);
        assertEquals(0x01, uint8(matrix.getByte(0)));
        assertEquals(0x01, matrix.getShort(0));

        // valid uint8 value with signed bit
        matrix.setInt(0, 0xFF);
        assertEquals(0xFF, uint8(matrix.getByte(0)));
        assertEquals(0xFF, matrix.getShort(0));

        // invalid value outside of range
        try {
            matrix.setInt(0, 0xFFFF);
            fail("Shouldn't be able to set the value");
        } catch (IllegalArgumentException ex) {
        }

    }

    @Test
    public void testNestedStruct() throws IOException {
        /*
         * Only root arrays need to have a name, so we can
         * omit names on variables that we know will be nested.
         * When a cell array or struct is created, non-set values
         * are automatically set to empty, which is equivalent to
         * MATLAB's [].
         */
        Matrix matrix = Mat5.newMatrix(32, 32);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                matrix.setDouble(row, col, row * 31 + col * 23);
            }
        }

        Cell cell = Mat5.newCell(1, 2)
                .set(0, 0, Mat5.EMPTY_MATRIX)
                .set(0, 1, matrix);

        Struct struct = Mat5.newStruct()
                .set("cel", cell)
                .set("name", Mat5.newString("single quoted string"))
                .set("scalar", Mat5.newScalar(27))
                .set("complexScalar", Mat5.newComplexScalar(27, 16));

        MatFile matFile = Mat5.newMatFile().addArray("root", struct);

        // Write/Read to file
        File file = new File("./temp-test-file.mat");
        try (Sink sink = Sinks.newStreamingFile(file)) {
            matFile.writeTo(sink);
        }

        final MatFile result;
        try (Source source = Sources.openFile(file)) {
            result = Mat5.newReader(source).readMat();
        }

        assertTrue("Could not delete temporary file", file.delete());

        // Access content. Nested classes have overloads to avoid boiler plate casting
        String actual = result.getStruct("root").getChar("name").getString();
        assertEquals("single quoted string", actual);

    }

    @Test
    public void testReadFilter() throws IOException {
        // Write dummy data with several root entries to buffer
        MatFile matFile = Mat5.newMatFile()
                .addArray("matrix", Mat5.newMatrix(Mat5.dims(2, 2, 1, 3)))
                .addArray("name", Mat5.newString("single quoted string"))
                .addArray("scalar", Mat5.newScalar(27))
                .addArray("complexScalar", Mat5.newComplexScalar(27, 16));

        ByteBuffer buffer = ByteBuffer.allocate(sint32(matFile.getUncompressedSerializedSize()));
        matFile.writeTo(Sinks.wrap(buffer).nativeOrder());
        buffer.flip();

        // Setup filter that only allows arrays that fulfill a certain criteria
        // Note that the filter only gets applied to the root entries, so entries
        // inside structs/cell arrays etc. don't get filtered.
        MatFile result = Mat5.newReader(Sources.wrap(buffer))
                .setEntryFilter(header -> header.getNumElements() == 1)
                .readMat();

        assertEquals(2, result.getNumEntries());
        result.getMatrix("scalar");
        result.getMatrix("complexScalar");

    }

    @Test
    public void testGlobalVariables() throws IOException {

        // Add a global variable to a mat file
        MatFile matFile = Mat5.newMatFile()
                .addArray("globalVar", true, Mat5.newScalar(1.0))
                .addArray("localVar", false, Mat5.newScalar(1.0));

        ByteBuffer buffer = ByteBuffer.allocate(sint32(matFile.getUncompressedSerializedSize()));
        matFile.writeTo(Sinks.wrap(buffer).nativeOrder());
        buffer.flip();

        MatFile result = Mat5.newReader(Sources.wrap(buffer)).readMat();

        // Create lookup table of all global variables
        HashMap<String, Array> globalVariables = new HashMap<>();
        for (MatFile.Entry entry : result.getEntries()) {
            if (entry.isGlobal())
                globalVariables.put(entry.getName(), entry.getValue());
        }

        assertNotNull(globalVariables.get("globalVar"));
        assertNull(globalVariables.get("localVar"));
    }

    @Test
    public void testIncrementalWritesWithMixedConcurrentCompression() throws IOException {
        // Create arrays without attaching them to a MatFile
        Array var1 = Mat5.newString("Test");
        Array var2 = Mat5.newScalar(7);
        Array var3 = Mat5.newMatrix(8, 8);
        Array var4 = Mat5.newChar(7, 7);

        // Pre-allocate a buffer to hold the results. Note that streaming files
        // would expand as needed, so calculating the size would not be required.
        // However, for unit tests it's better to keep things in memory. Since the
        // variables are not attached to a MatFile, we need to sum them manually.
        long maxExpectedSize = Mat5.FILE_HEADER_SIZE
                + Mat5.getSerializedSize("var1", var1)
                + Mat5.getSerializedSize("var2", var2)
                + Mat5.getSerializedSize("var3", var3)
                + Mat5.getSerializedSize("var4", var4);
        ByteBuffer buffer = ByteBuffer.allocate(sint32(maxExpectedSize));

        // Write header and add individual arrays with mixed compression (tested loading w/ R2017b)
        try (Sink sink = Sinks.wrap(buffer).nativeOrder()) {

            Mat5.newWriter(sink)
                    .writeMat(Mat5.newMatFile()) // new file has no content, so this just writes the header
                    .enableConcurrentCompression(executorService) // compress with multiple threads
                    .setDeflateLevel(Deflater.NO_COMPRESSION).writeArray("var1", var1)
                    .setDeflateLevel(Deflater.BEST_SPEED).writeArray("var2", var2)
                    .setDeflateLevel(Deflater.NO_COMPRESSION).writeArray("var3", var3)
                    .setDeflateLevel(Deflater.BEST_COMPRESSION).writeArray("var4", var4)
                    .flush(); // finish file

        }

        // Read mat file
        buffer.flip();
        try (Source source = Sources.wrap(buffer)) {

            Mat5File matFile = Mat5.newReader(source)
                    .enableConcurrentDecompression(executorService) // decompress with multiple threads
                    .setEntryFilter(header -> !"var3".equals(header.getName()))
                    .readMat();

            assertEquals("Test", matFile.getChar("var1").getString());
            assertEquals(((Matrix) var2).getLong(0), matFile.getMatrix("var2").getLong(0));
            assertArrayEquals(var4.getDimensions(), matFile.getChar("var4").getDimensions());

            try {
                matFile.getObject("var3");
                fail();
            } catch (IllegalArgumentException e) {
                // was filtered out when reading
            }

        }

    }

    @Test
    public void testAppendEntryToExistingFile() throws IOException {
        File file = new File("appending-test.mat");

        // Create new MAT File
        try (Sink sink = Sinks.newStreamingFile(file)) {
            Mat5.newMatFile()
                    .addArray("one", Mat5.newScalar(1))
                    .writeTo(sink);
        }

        // Read MAT
        try (Source source = Sources.openFile(file)) {
            MatFile matFile = Mat5.newReader(source).readMat();
            assertEquals(1, matFile.getNumEntries());
        }

        // Append new entry. The file already has a header,
        // so we only append the array itself.
        try (Sink sink = Sinks.newStreamingFile(file, true)) {
            Mat5.newWriter(sink)
                    .writeArray("two", Mat5.newScalar(2))
                    .setDeflateLevel(Deflater.NO_COMPRESSION)
                    .writeArray("three", Mat5.newScalar(3));
        }

        // Read MAT
        try (Source source = Sources.openFile(file)) {
            MatFile matFile = Mat5.newReader(source).readMat();
            assertEquals(3, matFile.getNumEntries());
        }

        assertTrue(file.delete());

    }

    private static double DELTA = 0;
    private static ExecutorService executorService = Executors.newCachedThreadPool();

}
