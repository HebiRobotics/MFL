package us.hebi.matlab.io.types;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Serves as a source for reading MAT files. Actual source
 * may be fiels, buffers, memory-mapped files, InputStreams,
 * etc.
 * <p>
 * All methods read exactly the specified number of bytes,
 * or throw an EOFException
 *
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public interface Source extends Closeable {

    Source order(ByteOrder byteOrder);

    ByteOrder order();

    /**
     * @return current position, i.e., the number of bytes that have been read from this source
     */
    long getPosition();

    byte readByte() throws IOException;

    void readBytes(byte[] buffer, int offset, int length) throws IOException;

    short readShort() throws IOException;

    void readShorts(short[] buffer, int offset, int length) throws IOException;

    int readInt() throws IOException;

    void readInts(int[] buffer, int offset, int length) throws IOException;

    long readLong() throws IOException;

    void readLongs(long[] buffer, int offset, int length) throws IOException;

    float readFloat() throws IOException;

    void readFloats(float[] buffer, int offset, int length) throws IOException;

    double readDouble() throws IOException;

    void readDoubles(double[] buffer, int offset, int length) throws IOException;

    /**
     * Creates a new source that reads up to the specified number of compressed bytes.
     * The new source has the same byte order as the underlying one.
     * <p>
     * Note that closing the returned source does not close the parent source.
     *
     * @param numBytes          maximum number of bytes that may be read from this source
     * @param inflateBufferSize inflate buffer size
     * @return child source that reads deflated data from this source
     * @throws IOException if read errors occur
     */
    Source readInflated(int numBytes, int inflateBufferSize) throws IOException;

    /**
     * Checks whether reading from a child source created by {#see readInflated} mutates this source.
     * If true, then the child source must be read in the same thread as this source, and reads must
     * be halted until the child source has been fully read.
     * <p>
     * Returning false means that child sources are independent and can be read from concurrently, i.e.,
     * the decompression may be done on a different thread.
     *
     * @return true if a Source created by {@link Source#readInflated} may be read by a different thread
     */
    boolean isMutatedByChildren();

    /**
     * Same behavior as
     * <pre>{@code
     * while(byteBuffer.remaining() > 0){
     *     byteBuffer.put(readByte());
     * }
     * }</pre>
     *
     * @param buffer buffer
     * @throws IOException if read errors occur
     */
    void readByteBuffer(ByteBuffer buffer) throws IOException;

    /**
     * Skips exactly the specified number of bytes or throws an EOFException
     *
     * @param numBytes number of bytes to be skipped
     * @throws IOException EOF if there is not enough data left
     */
    void skip(long numBytes) throws IOException;

}
