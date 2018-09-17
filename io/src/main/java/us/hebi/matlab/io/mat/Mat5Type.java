package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.MatlabType;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.common.util.Casts;

import java.io.IOException;
import java.nio.ByteBuffer;

import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * Represents the type that the data is physically stored inside
 * a mat file. Note that this may be different from the array type
 * that users see after loading a file, e.g., UInt64 data could be
 * stored as UInt8 if all variables are within range.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 20 Apr 2018
 */
public enum Mat5Type {
    Int8(1), // miINT8
    UInt8(2),
    Int16(3),
    UInt16(4),
    Int32(5),
    UInt32(6),
    Single(7),
    // 8 reserved
    Double(9),
    // 10 reserved
    // 11 reserved
    Int64(12),
    UInt64(13),
    Matrix(14),
    Compressed(15),
    Utf8(16),
    Utf16(17),
    Utf32(18);

    void writeByteBufferWithTag(ByteBuffer buffer, Sink sink) throws IOException {
        if (bytes() > 1 && buffer.order() != sink.getByteOrder())
            throw new IllegalArgumentException("Buffer order does not match sink order");

        int position = buffer.position();
        int numElements = buffer.remaining() / bytes();
        writeTag(numElements, sink);
        sink.writeByteBuffer(buffer);
        writePadding(numElements, sink);
        buffer.position(position);
    }

    void writeBytesWithTag(byte[] values, Sink sink) throws IOException {
        if (bytes() != SIZEOF_BYTE)
            throw new IllegalArgumentException("Not a byte tag type");

        writeTag(values.length, sink);
        sink.writeBytes(values, 0, values.length);
        writePadding(values.length, sink);
    }

    void writeIntsWithTag(int[] values, Sink sink) throws IOException {
        if (this != Int32 && this != UInt32)
            throw new IllegalArgumentException("Not an integer tag type");

        writeTag(values.length, sink);
        sink.writeInts(values, 0, values.length);
        writePadding(values.length, sink);
    }

    /**
     * @return serialized size including tag, data, and padding
     */
    public int computeSerializedSize(int numElements) {
        int numBytes = getNumBytes(numElements);
        boolean packed = isPackable(numBytes);
        int tagSize = packed ? 4 : 8;
        int padding = getPadding(numBytes, packed);
        return tagSize + numBytes + padding;
    }

    public void writeTag(int numElements, boolean allowPacking, Sink sink) throws IOException {
        int numBytes = getNumBytes(numElements);
        if (allowPacking && isPackable(numBytes)) {
            sink.writeInt(numBytes << 16 | id());
        } else {
            sink.writeInt(id());
            sink.writeInt(numBytes);
        }
    }

    public void writeTag(int numElements, Sink sink) throws IOException {
        this.writeTag(numElements, true, sink);
    }

    public void writePadding(int numElements, Sink sink) throws IOException {
        int numBytes = getNumBytes(numElements);
        int padding = getPadding(numBytes, isPackable(numBytes));
        if (padding == 0) return;
        sink.writeBytes(paddingBuffer, 0, padding);
    }

    int getPadding(int numBytes, boolean packed) {
        switch (this) {
            case Matrix: // already aligned
            case Compressed: // no alignment needed
                return 0;
            default:
                int tagSize = packed ? 4 : 8;
                int padding = (tagSize + numBytes) % 8;
                return padding == 0 ? 0 : 8 - padding;
        }
    }

    private int getNumBytes(long numElements) {
        return Casts.sint32(bytes() * numElements);
    }

    /**
     * If a data element takes up only 1 to 4 bytes, MATLAB saves storage space by storing the
     * data in an 8-byte format. In this format, the Data Type and Number of Bytes fields are
     * stored as 16-bit values, freeing 4 bytes in the tag in which to store the data.
     */
    private static boolean isPackable(int numBytes) {
        return numBytes <= 4;
    }

    public int bytes() {
        switch (this) {
            case Int8:
            case UInt8:
                return SIZEOF_BYTE;
            case Int16:
            case UInt16:
                return SIZEOF_SHORT;
            case Int32:
            case UInt32:
            case Single:
                return SIZEOF_INT;
            case Int64:
            case UInt64:
            case Double:
                return SIZEOF_LONG;
            default:
                return SIZEOF_BYTE;
        }
    }

    public static Mat5Type fromNumericalType(MatlabType type) {
        switch (type) {
            case Int8:
                return Mat5Type.Int8;
            case Int16:
                return Mat5Type.Int16;
            case Int32:
                return Mat5Type.Int32;
            case Int64:
                return Mat5Type.Int64;
            case UInt8:
                return Mat5Type.UInt8;
            case UInt16:
                return Mat5Type.UInt16;
            case UInt32:
                return Mat5Type.UInt32;
            case UInt64:
                return Mat5Type.UInt64;
            case Single:
                return Mat5Type.Single;
            case Double:
                return Mat5Type.Double;
            default:
                throw new IllegalArgumentException("Not a numerical type: " + type);
        }
    }

    public CharEncoding getCharEncoding() {
        switch (this) {
            case Utf8:
                return CharEncoding.Utf8;
            case Utf16:
                return CharEncoding.Utf16;
            case Utf32:
                return CharEncoding.Utf32;
            case UInt16:
                return CharEncoding.UInt16;
            default:
                throw new IllegalArgumentException("Not a character type: " + this);
        }
    }

    public static Mat5Type fromCharEncoding(CharEncoding type) {
        switch (type) {
            case Utf8:
                return Utf8;
            case Utf16:
                return Utf16;
            case Utf32:
                return Utf32;
            case UInt16:
                return UInt16;
            default:
                throw new IllegalArgumentException("Unknown char type " + type);
        }
    }

    public static Mat5Type fromId(int id) {
        if (id > 0 && id < lookup.length) {
            Mat5Type type = lookup[id];
            if (type != null)
                return type;
        }
        throw new IllegalArgumentException("Unknown tag type for id: " + id);
    }

    public int id() {
        return id;
    }

    private Mat5Type(int id) {
        this.id = id;
    }

    private final int id;


    private static final Mat5Type[] lookup;

    static {
        // Determine size of lookup table
        int highestId = 0;
        for (Mat5Type type : values()) {
            highestId = Math.max(highestId, type.id);
        }

        // Populate lookup table
        lookup = new Mat5Type[highestId + 1];
        for (Mat5Type type : values()) {
            lookup[type.id()] = type;
        }
    }

    private static final byte[] paddingBuffer = new byte[8];

}
