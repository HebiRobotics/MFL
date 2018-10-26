package us.hebi.matlab.io.mat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import us.hebi.matlab.io.mat.mat5test.MatTestUtil;
import us.hebi.matlab.io.types.*;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import static us.hebi.matlab.io.mat.Mat5WriteUtil.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 07 May 2018
 */
public class Mat5WriterTest {

    static ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 1024);
    static Sink sink = Sinks.wrap(buffer).nativeOrder();

    static Mat5Writer rawWriter = Mat5.newWriter(sink).setDeflateLevel(Deflater.NO_COMPRESSION);
    static Mat5Writer compressedWriter = Mat5.newWriter(sink).setDeflateLevel(Deflater.BEST_SPEED);

    @Before
    public void rewind() {
        buffer.clear();
    }

    @Test
    public void computeSerializedSizeDoubleNDim() throws Exception {
        checkSerializedSize("arrays/multiDimMatrix.mat", "in");
    }

    @Test
    public void computeSerializedSizeDouble2Dim() throws Exception {
        checkSerializedSize("arrays/matnativedouble.mat", "arr");
    }

    @Test
    public void computeSerializedSizeInt32() throws Exception {
        checkSerializedSize("arrays/int32.mat", "a");
    }

    @Test
    public void computeSerializedSizeInt64() throws Exception {
        checkSerializedSize("arrays/int64.mat", "arr");
    }

    @Test
    public void computeSerializedSizeSparse() throws Exception {
        checkSerializedSize("arrays/sparse.mat", "spa");
    }

    @Test
    public void computeSerializedSizeCell() throws Exception {
        Cell name = MatTestUtil.readMat("arrays/cell.mat")
                .getStruct("cel")
                .getStruct("xX")
                .getCell("name");
        checkSerializedSizeWithMessage("cell.mat:cel:xX:name", name);
    }

    @Test
    public void computeSerializedSizeSimpleStruct() throws Exception {
        Struct struct = MatTestUtil.readMat("arrays/simplestruct.mat").getStruct("structure");
        checkSerializedSizeWithMessage("double array", struct.get("a", 0, 0));
        checkSerializedSizeWithMessage("char array", struct.get("string", 1, 0));
        checkSerializedSizeWithMessage("struct array", struct);
    }

    @Test
    public void computeSerializedSizeComplexStruct() throws Exception {
        checkSerializedSize("arrays/cell.mat", "cel");
    }

    @Test
    public void computeSerializedSizeChar() throws Exception {
        Struct struct = MatTestUtil.readMat("arrays/simplestruct.mat").getStruct("structure");
        Char string = struct.get("string", 0, 0);
        checkSerializedSizeWithMessage("arrays/simplestruct.mat::structure::name", string);
    }

    @Test
    public void computeSerializedSizeOfComplexMatFile() throws Exception {
        MatFile matFile = MatTestUtil.readMat("arrays/cell.mat");
        long calculatedSize = matFile.getUncompressedSerializedSize();
        rawWriter.writeMat(matFile);
        long actualSize = buffer.position();
        Assert.assertEquals("Uncompressed File", calculatedSize, actualSize);
    }

    private void checkSerializedSize(String matFile, String arrayName) throws Exception {
        Array array = MatTestUtil.readMat(matFile).getArray(arrayName);
        checkSerializedSize(matFile + "::" + arrayName, arrayName, array);
    }

    private void checkSerializedSizeWithMessage(String message, Array array) throws Exception {
        checkSerializedSize(message, "test", array);
    }

    private void checkSerializedSize(String message, String arrayName, Array array) throws Exception {
        buffer.rewind();
        long calculatedSize = computeArraySize(arrayName, array);
        long offset = sink.position();
        writeArrayWithTag(arrayName, array, sink);
        long actualSize = sink.position() - offset;
        Assert.assertEquals(message, calculatedSize, actualSize);
    }

    @Test
    public void readWriteDoubleNDim() throws Exception {
        MatFile input = MatTestUtil.readMat("arrays/multiDimMatrix.mat");
        compressedWriter.writeMat(input);
        buffer.flip();
        MatFile output = Mat5.newReader(Sources.wrap(buffer)).readMat();

        Matrix expected = input.getArray("in");
        Matrix actual = output.getArray("in");

        Assert.assertEquals(expected.getType(), actual.getType());
        Assert.assertArrayEquals(expected.getDimensions(), actual.getDimensions());

        // Access sequentially using raw store
        int numElements = expected.getNumElements();
        for (int i = 0; i < numElements; i++) {
            Assert.assertEquals(expected.getDouble(i), actual.getDouble(i), 0);
        }

    }

}