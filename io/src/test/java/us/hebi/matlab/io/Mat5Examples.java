package us.hebi.matlab.io;

import org.junit.Test;
import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.mat.Mat5Reader;
import us.hebi.matlab.io.mat.Mat5Writer;
import us.hebi.matlab.io.types.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Deflater;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.util.Casts.*;

/**
 * API Examples for creating MAT 5 files
 *
 * @author Florian Enner < florian @ hebirobotics.com >
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
        MatFile result = Mat5.newReader(Sources.wrap(buffer)).readFile();

        // Access data
        assertEquals(2, result.getMatrix("identityMatrix").getLong(1, 1));

    }

    @Test
    public void writeFullFile() throws IOException {
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
        Sink sink = Sinks.wrap(buffer);
        try {

            // Write file w/ header and content
            matFile.writeTo(sink);

        } finally {
            // NO-OP for heap-buffer, but included for demonstration
            sink.close();
        }
    }

    @Test
    public void writeIncrementalWithMixedCompression() throws IOException {
        // Create arrays without attaching them to a MatFile
        Array var1 = Mat5.newString("Test");
        Array var2 = Mat5.newScalar(7);
        Array var3 = Mat5.newMatrix(8, 8);
        Array var4 = Mat5.newChar(7, 7);

        // Calculate max expected size assuming that compression will
        // reduce the result
        long maxExpectedSize = Mat5.FILE_HEADER_SIZE
                + Mat5Writer.computeArraySize("var1", var1)
                + Mat5Writer.computeArraySize("var2", var2)
                + Mat5Writer.computeArraySize("var3", var3)
                + Mat5Writer.computeArraySize("var4", var4);

        // Write to a pre-allocated buffer. Note that for a streaming file we
        // would not need to calculate the size
        ByteBuffer buffer = ByteBuffer.allocate(sint32(maxExpectedSize));
        Sink sink = Sinks.wrap(buffer);
        try {

            // Write header and add individual arrays with mixed compression (tested loading w/ R2017b)
            Mat5.newWriter(sink)
                    .writeFile(Mat5.newMatFile()) // new file has no content, so this just writes the header
                    .setDeflateLevel(Deflater.NO_COMPRESSION).writeRootArray("var1", var1)
                    .setDeflateLevel(Deflater.BEST_SPEED).writeRootArray("var2", var2)
                    .setDeflateLevel(Deflater.NO_COMPRESSION).writeRootArray("var3", var3)
                    .setDeflateLevel(Deflater.BEST_COMPRESSION).writeRootArray("var4", var4);

        } finally {
            // NO-OP for heap-buffer, but included for demonstration
            sink.close();
        }
    }

    @Test
    public void testDoubleMatrix() {
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
        Sink sink = Sinks.newStreamingFile(file);
        matFile.writeTo(sink);
        sink.close();

        Source source = Sources.openFile(file);
        MatFile result = Mat5.newReader(source).readFile();
        source.close();

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
        matFile.writeTo(Sinks.wrap(buffer).setByteOrder(ByteOrder.nativeOrder()));
        buffer.flip();

        // Setup filter that only allows arrays that fulfill a certain criteria
        // Note that the filter only gets applied to the root entries, so entries
        // inside structs/cell arrays etc. don't get filtered.
        Mat5Reader.ArrayFilter filter = new Mat5Reader.ArrayFilter() {
            @Override
            public boolean isAccepted(Mat5Reader.ArrayHeader header) {
                // 2D arrays with an "x" or "n" in the name
                String name = header.getName();
                return (name.contains("x") || name.contains("a"))
                        && header.getDimensions().length == 2;
            }
        };

        MatFile result = Mat5.newReader(Sources.wrap(buffer))
                .setArrayFilter(filter)
                .readFile();

        assertEquals(3, result.size());
        result.getChar("name");
        result.getMatrix("scalar");
        result.getMatrix("complexScalar");

    }

    private static double DELTA = 0;

}
