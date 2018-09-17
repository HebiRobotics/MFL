package us.hebi.matlab.io.types;

import us.hebi.matlab.common.memory.ByteConverter;
import us.hebi.matlab.common.memory.ByteConverters;
import us.hebi.matlab.common.memory.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 26 Aug 2018
 */
public abstract class AbstractSource implements Source {

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    @Override
    public ByteOrder getByteOrder() {
        if (byteOrder == null) {
            throw new IllegalStateException("Byte order has not been initialized");
        }
        return byteOrder;
    }

    @Override
    public byte readByte() throws IOException {
        readBytes(bytes, 0, 1);
        return bytes[0];
    }

    @Override
    public short readShort() throws IOException {
        readBytes(bytes, 0, SIZEOF_SHORT);
        return byteConverter.getShort(getByteOrder(), bytes, 0);
    }

    @Override
    public int readInt() throws IOException {
        readBytes(bytes, 0, SIZEOF_INT);
        return byteConverter.getInt(getByteOrder(), bytes, 0);
    }

    @Override
    public long readLong() throws IOException {
        readBytes(bytes, 0, SIZEOF_LONG);
        return byteConverter.getLong(getByteOrder(), bytes, 0);
    }

    @Override
    public float readFloat() throws IOException {
        readBytes(bytes, 0, SIZEOF_FLOAT);
        return byteConverter.getFloat(getByteOrder(), bytes, 0);
    }

    @Override
    public double readDouble() throws IOException {
        readBytes(bytes, 0, SIZEOF_DOUBLE);
        return byteConverter.getDouble(getByteOrder(), bytes, 0);
    }

    @Override
    public void readByteBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.hasArray()) {
            // Fast path
            int offset = buffer.arrayOffset() + buffer.position();
            int length = buffer.remaining();
            readBytes(buffer.array(), offset, length);
            buffer.position(buffer.limit());
        } else {
            // Slow path
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), bytes.length);
                readBytes(bytes, 0, length);
                buffer.put(bytes, 0, length);
            }
        }
    }

    @Override
    public void readShorts(short[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_SHORT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_SHORT, i++) {
                buffer[offset + i] = byteConverter.getShort(getByteOrder(), bytes, j);
            }
        }
    }

    @Override
    public void readInts(int[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_INT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_INT, i++) {
                buffer[offset + i] = byteConverter.getInt(getByteOrder(), bytes, j);
            }
        }
    }

    @Override
    public void readLongs(long[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_LONG, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_LONG, i++) {
                buffer[offset + i] = byteConverter.getLong(getByteOrder(), bytes, j);
            }
        }
    }

    @Override
    public void readFloats(float[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_FLOAT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_FLOAT, i++) {
                buffer[offset + i] = byteConverter.getFloat(getByteOrder(), bytes, j);
            }
        }
    }

    @Override
    public void readDoubles(double[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_DOUBLE, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_DOUBLE, i++) {
                buffer[offset + i] = byteConverter.getDouble(getByteOrder(), bytes, j);
            }
        }
    }

    @Override
    public void skip(long numBytes) throws IOException {
        long n = 0;
        while (n < numBytes) {
            long count = Math.min(numBytes - n, bytes.length);
            readBytes(bytes, 0, (int) count);
            n += count;
        }
    }

    @Override
    public Source readInflated(int numBytes, int inflateBufferSize) throws IOException {
        InputStream subInputStream = readBytesAsStream(numBytes);
        InputStream inflaterInput = new InflaterInputStream(subInputStream, new Inflater(), inflateBufferSize);
        Source matInputBuffer = Sources.wrapInputStream(inflaterInput, bytes.length);
        matInputBuffer.setByteOrder(getByteOrder());
        return matInputBuffer;
    }

    /**
     * @return stream that reads up to the number of specified bytes. Close() shall not close this source
     */
    protected abstract InputStream readBytesAsStream(long numBytes) throws IOException;

    protected AbstractSource(int bufferSize) {
        // Make sure size is always a multiple of 8, and that it can hold the 116 byte description
        int size = Math.max(Bytes.nextPowerOfTwo(bufferSize), 128);
        this.bytes = new byte[size];
    }

    private ByteOrder byteOrder = null;
    private final byte[] bytes;
    private static final ByteConverter byteConverter = ByteConverters.getFastest();

}
