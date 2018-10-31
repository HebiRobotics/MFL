package us.hebi.matlab.mat.types;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Deflater;

/**
 * Serves as a target for writing Mat files. Targets can include
 * files, buffers, memory-mapped files, OutputStreams, etc.
 *
 * @author Florian Enner
 * @since 06 May 2018
 */
public interface Sink extends Closeable {

    /**
     * Sets the byte order of this sink to the native order
     * of the current platform. This matches the MATLAB behavior
     * for writing MAT files.
     * <p>
     * While this is not strictly required, there is no downside,
     * and writing native order may be slightly faster.
     * <p>
     * Equivalent to .order(ByteOrder.nativeOrder())
     *
     * @return this
     */
    Sink nativeOrder();

    Sink order(ByteOrder byteOrder);

    ByteOrder order();

    /**
     * @return current position
     */
    long position() throws IOException;

    /**
     * May not be supported by all targets. Only needed for
     * root sink when writing compressed files.
     *
     * @param position moves to the desired position
     */
    void position(long position) throws IOException;

    void writeByte(byte value) throws IOException;

    void writeBytes(byte[] buffer, int offset, int length) throws IOException;

    void writeShort(short value) throws IOException;

    void writeShorts(short[] buffer, int offset, int length) throws IOException;

    void writeInt(int value) throws IOException;

    void writeInts(int[] buffer, int offset, int length) throws IOException;

    void writeLong(long value) throws IOException;

    void writeLongs(long[] buffer, int offset, int length) throws IOException;

    void writeFloat(float value) throws IOException;

    void writeFloats(float[] buffer, int offset, int length) throws IOException;

    void writeDouble(double value) throws IOException;

    void writeDoubles(double[] buffer, int offset, int length) throws IOException;

    /**
     * Returns a child sink that writes the added data compressed into this sink. Closing the
     * child sink results in the data being flushed, but does not close this sink. The child sink
     * has to be closed before writes to this sink can continue.
     * <p>
     * Note that the child sink's byte order is the same as the parent's.
     *
     * @param deflater deflater
     * @return child sink that writes deflated data into this sink
     */
    Sink writeDeflated(Deflater deflater);

    /**
     * Same behavior as
     *
     * <pre>{@code
     * while(byteBuffer.remaining() > 0){
     *     writeByte((byte) byteBuffer.get());
     * }
     * }</pre>
     *
     * @param buffer buffer
     */
    void writeByteBuffer(ByteBuffer buffer) throws IOException;

    /**
     * Reads N bytes from the input stream and writes them into this sink
     *
     * @param input input source
     * @param length number of bytes to be read
     * @throws IOException EOF if the source failed
     */
    void writeInputStream(InputStream input, long length) throws IOException;

    /**
     * Reads N bytes from the DataInput and writes them into this sink
     *
     * @param input input source
     * @param length number of bytes to be read
     * @throws IOException EOF if the data input does not have enough data
     */
    void writeDataInput(DataInput input, long length) throws IOException;

}
