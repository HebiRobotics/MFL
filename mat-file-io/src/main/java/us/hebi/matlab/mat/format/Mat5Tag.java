package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.Source;

import java.io.EOFException;
import java.io.IOException;

import static us.hebi.matlab.common.memory.Bytes.*;
import static us.hebi.matlab.mat.format.Mat5Reader.*;

/**
 * --- Data Element Format ---
 * Each data element begins with an 8-byte tag followed immediately by the data in the
 * element. The data is then followed by padding up to 8 bytes. The Matrix and Compressed
 * types do not require padding.
 * <p>
 * [4 byte data type]
 * [4 byte number of bytes]
 * [N byte ... data ...]
 * [0-7 byte padding]
 * <p>
 * --- Small Data Element Format ---
 * If a data element takes up only 1 to 4 bytes, MATLAB saves storage space by storing the
 * data in an 8-byte format. In this format, the Data Type and Number of Bytes fields are
 * stored as 16-bit values, freeing 4 bytes in the tag in which to store the data.
 * <p>
 * [2 byte number of bytes]
 * [2 byte data type]
 * [N byte ... data ...]
 * [0-7 byte padding]
 * <p>
 * @see <a href="http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf">MAT-File Format</a>
 *
 * @author Florian Enner
 * @since 30 Apr 2018
 */
public class Mat5Tag {

    public Mat5Type getType() {
        return type;
    }

    public int getNumBytes() {
        return numBytes;
    }

    public int getNumElements() {
        return numBytes / type.bytes();
    }

    public int getPadding() {
        return type.getPadding(numBytes, packed);
    }

    /**
     * @return next tag, or null if the source is at the end
     */
    public static Mat5Tag readTagOrNull(Source source) throws IOException {
        try {
            return readTag(source);
        } catch (EOFException eof) {
            return null;
        }
    }

    /**
     * @return next tag, or EOF Exception if the source is at the end
     */
    public static Mat5Tag readTag(Source source) throws IOException {
        final int tmp = source.readInt();
        final Mat5Type type;
        final int numBytes;

        // Packed/Compacted header
        final boolean packed = tmp >> 16 != 0;
        if (!packed) {
            // not packed (8 bytes)
            type = Mat5Type.fromId(tmp);
            numBytes = source.readInt();
        } else {
            // packed (4 bytes)
            numBytes = tmp >> 16; // upper
            type = Mat5Type.fromId(tmp & 0xFFFF); // lower
        }

        // Sanity check that byte size matches tag
        if (numBytes % type.bytes() != 0)
            throw readError("Found invalid number of bytes for tag '%s'. Expected multiple of %d. Found %d",
                    type, type.bytes(), numBytes);

        return new Mat5Tag(type, numBytes, packed, source);
    }

    byte[] readAsBytes() throws IOException {
        byte[] buffer = new byte[getNumBytes()];
        source.readBytes(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    short[] readAsShorts() throws IOException {
        checkMultipleOf(SIZEOF_SHORT, "short[]");
        short[] buffer = new short[getNumBytes() / SIZEOF_SHORT];
        source.readShorts(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    int[] readAsInts() throws IOException {
        checkMultipleOf(SIZEOF_INT, "int[]");
        int[] buffer = new int[getNumBytes() / SIZEOF_INT];
        source.readInts(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    long[] readAsLongs() throws IOException {
        checkMultipleOf(SIZEOF_LONG, "long[]");
        long[] buffer = new long[getNumBytes() / SIZEOF_LONG];
        source.readLongs(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    float[] readAsFloats() throws IOException {
        checkMultipleOf(SIZEOF_FLOAT, "float[]");
        float[] buffer = new float[getNumBytes() / SIZEOF_FLOAT];
        source.readFloats(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    double[] readAsDoubles() throws IOException {
        checkMultipleOf(SIZEOF_DOUBLE, "double[]");
        double[] buffer = new double[getNumBytes() / SIZEOF_DOUBLE];
        source.readDoubles(buffer, 0, buffer.length);
        source.skip(getPadding());
        return buffer;
    }

    private void checkMultipleOf(int bytes, String target) throws IOException {
        if (getNumBytes() % bytes != 0) {
            throw readError("Tag with %d bytes cannot be read as %s", bytes, target);
        }
    }


    @Override
    public String toString() {
        return "Mat5Tag{" +
                "type=" + type +
                ", numBytes=" + numBytes +
                (packed ? " (packed)" : "") +
                '}';
    }

    private Mat5Tag(Mat5Type type, int numBytes, boolean packed, Source source) {
        this.type = type;
        this.numBytes = numBytes;
        this.packed = packed;
        this.source = source;
    }

    private final Mat5Type type;

    private final int numBytes;

    private final boolean packed;

    private final Source source;

}
