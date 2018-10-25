package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.util.Casts;
import us.hebi.matlab.common.util.Charsets;
import us.hebi.matlab.io.types.*;
import us.hebi.matlab.io.types.Opaque;

import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.zip.Deflater;

import static us.hebi.matlab.common.util.Casts.checkedDivide;
import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.io.mat.Mat5.*;
import static us.hebi.matlab.io.mat.Mat5Type.*;
import static us.hebi.matlab.io.mat.Mat5Type.Int32;
import static us.hebi.matlab.io.mat.Mat5Type.Int8;
import static us.hebi.matlab.io.mat.Mat5Type.UInt32;
import static us.hebi.matlab.io.types.MatlabType.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 May 2018
 */
public class Mat5Writer implements Closeable {

    /**
     * Computes the resulting file size if compression is disabled. Since
     * compression usually reduces the file size, this can be seen as a
     * maximum expected size.
     * <p>
     * This is useful to e.g. pre-allocate a buffer or file that can be
     * truncated once the actual file size is known.
     * <p>
     * Note that it is not guaranteed that compression will result in a
     * smaller file size, e.g., we have seen this happen on small arrays
     * with little data. Thus, for small arrays, you should add padding.
     */
    public static long computeUncompressedSize(MatFile matFile) {
        long size = FILE_HEADER_SIZE;
        for (NamedArray entry : matFile) {
            size += computeArraySize(entry.getName(), entry.getValue());
        }
        return size;
    }

    protected Mat5Writer(Sink sink) {
        this.sink = checkNotNull(sink);
    }

    public Mat5Writer setDeflateLevel(int deflateLevel) {
        this.deflateLevel = deflateLevel;
        return this;
    }

    public Mat5Writer writeFile(MatFile matFile) throws IOException {
        if (matFile instanceof Mat5File) {
            return writeFile((Mat5File) matFile);
        }
        throw new IllegalArgumentException("MatFile does not support the MAT5 format");
    }

    public Mat5Writer writeFile(Mat5File matFile) throws IOException {
        long headerStart = sink.position();
        matFile.writeFileHeader(sink);
        for (NamedArray namedArray : matFile) {
            long entryStart = sink.position();
            writeRootArray(namedArray);

            // Update subsystem location in file header
            if (namedArray.getValue() instanceof Mat5Subsystem) {
                matFile.updateSubsysOffset(headerStart, entryStart, sink);
            }

        }
        return this;
    }

    public Mat5Writer writeRootArray(NamedArray namedArray) throws IOException {
        return writeRootArray(namedArray.getName(), namedArray.getValue());
    }

    public Mat5Writer writeRootArray(String name, Array array) throws IOException {
        if ((name == null || name.isEmpty())
                && !(array instanceof McosReference)
                && !(array instanceof Mat5Subsystem))
            throw new IllegalArgumentException("Root Array can't have an empty name");

        // Disabled compression
        if (deflateLevel == Deflater.NO_COMPRESSION) {
            writeArrayWithTag(name, array, sink); // note: padding is included in size
            return this;
        }

        // Write placeholder tag with a dummy size so we can fill in info later
        long tagPosition = sink.position();
        Compressed.writeTag(DUMMY_SIZE, false, sink);
        long start = sink.position();

        // Compress matrix data
        Sink compressed = sink.writeDeflated(new Deflater(deflateLevel));
        writeArrayWithTag(name, array, compressed);
        compressed.close(); // triggers flush/finish

        // Calculate actual size
        long end = sink.position();
        long compressedSize = end - start;

        // Overwrite placeholder tag with the real size
        sink.position(tagPosition);
        Compressed.writeTag(Casts.sint32(compressedSize), false, sink);

        // Return to the current real position after the compressed data
        // Note that compressed tags don't require padding for alignment.
        sink.position(end);

        return this;

    }

    @Override
    public void close() throws IOException {
        sink.close();
    }

    public static void writeArrayWithTag(Array array, Sink sink) throws IOException {
        writeArrayWithTag("", array, sink);
    }

    public static void writeArrayWithTag(String name, Array array, Sink sink) throws IOException {
        if (array instanceof Mat5Serializable) {
            ((Mat5Serializable) array).writeMat5(name, sink);
            return;
        }
        throw new IllegalArgumentException("Array does not support the MAT5 format");
    }

    public static int computeArraySize(Array array) {
        return computeArraySize("", array);
    }

    public static int computeArraySize(String name, Array array) {
        if (array instanceof Mat5Serializable)
            return ((Mat5Serializable) array).getMat5Size(name);
        throw new IllegalArgumentException("Array does not support the MAT5 format");
    }

    public static int computeArrayHeaderSize(String name, Array array) {
        int arrayFlags = UInt32.computeSerializedSize(2);
        int dimensions = Int32.computeSerializedSize(array.getNumDimensions());
        int nameLen = Int8.computeSerializedSize(name.length()); // ascii
        return arrayFlags + dimensions + nameLen;
    }

    public static void writeMatrixTag(String name, Mat5Serializable array, Sink sink) throws IOException {
        Matrix.writeTag(array.getMat5Size(name) - Mat5.MATRIX_TAG_SIZE, sink);
    }

    public static void writeArrayHeader(String name, Array array, Sink sink) throws IOException {
        if (array.getType() == Opaque)
            throw new IllegalArgumentException("Opaque types do not share the same format as other types");

        // Subfield 1: Meta data
        UInt32.writeIntsWithTag(Mat5ArrayFlags.forArray(array), sink);

        // Subfield 2: Dimensions
        Int32.writeIntsWithTag(array.getDimensions(), sink);

        // Subfield 3: Name
        Int8.writeBytesWithTag(name.getBytes(Charsets.US_ASCII), sink);
    }

    public static int computeOpaqueSize(String name, Opaque array) {
        return Mat5.MATRIX_TAG_SIZE
                + UInt32.computeSerializedSize(2)
                + Int8.computeSerializedSize(name.length())
                + Int8.computeSerializedSize(array.getObjectType().length())
                + Int8.computeSerializedSize(array.getClassName().length())
                + Mat5Writer.computeArraySize(array.getContent());
    }

    public static void writeOpaque(String name, Opaque opaque, Sink sink) throws IOException {
        // Tag
        final int numBytes = computeOpaqueSize(name, opaque) - Mat5.MATRIX_TAG_SIZE;
        Matrix.writeTag(numBytes, sink);

        // Subfield 1: Meta data
        UInt32.writeIntsWithTag(Mat5ArrayFlags.forOpaque(opaque), sink);

        // Subfield 2: Ascii variable name
        Int8.writeBytesWithTag(name.getBytes(Charsets.US_ASCII), sink);

        // Subfield 3: Object Identifier (e.g. "MCOS", "handle", "java")
        Int8.writeBytesWithTag(opaque.getObjectType().getBytes(Charsets.US_ASCII), sink);

        // Subfield 4: Class name (e.g. "table", "string", "java.io.File")
        Int8.writeBytesWithTag(opaque.getClassName().getBytes(Charsets.US_ASCII), sink);

        // Subfield 5: Content
        Mat5Writer.writeArrayWithTag(opaque.getContent(), sink);
    }

    static int computeCharBufferSize(CharEncoding encoding, CharBuffer buffer) {
        Mat5Type tagType = Mat5Type.fromCharEncoding(encoding);
        int numElements = checkedDivide(encoding.getEncodedLength(buffer), tagType.bytes());
        return tagType.computeSerializedSize(numElements);
    }

    static void writeCharBufferWithTag(CharEncoding encoding, CharBuffer buffer, Sink sink) throws IOException {
        Mat5Type tagType = Mat5Type.fromCharEncoding(encoding);
        int numElements = checkedDivide(encoding.getEncodedLength(buffer), tagType.bytes());
        tagType.writeTag(numElements, sink);
        encoding.writeEncoded(buffer, sink);
        tagType.writePadding(numElements, sink);
    }

    private void checkType(Array element, MatlabType expected) {
        if (element.getType() != expected) {
            String format = String.format("%s is an invalid type. Expected %s", element.getType(), expected);
            throw new IllegalArgumentException(format);
        }
    }

    protected final Sink sink;
    private int deflateLevel = Deflater.BEST_SPEED;
    private static final int DUMMY_SIZE = 0;

}
