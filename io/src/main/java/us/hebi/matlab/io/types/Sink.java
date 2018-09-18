package us.hebi.matlab.io.types;

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
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 May 2018
 */
public interface Sink extends Closeable {

    Sink setByteOrder(ByteOrder byteOrder);

    ByteOrder getByteOrder();

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
     * @param deflater
     * @return
     */
    Sink writeDeflated(Deflater deflater);

    /**
     * Same behavior as
     * <p>
     * while(byteBuffer.remaining() > 0){
     * writeByte((byte) byteBuffer.get());
     * }
     *
     * @param buffer
     * @throws IOException
     */
    void writeByteBuffer(ByteBuffer buffer) throws IOException;

    /**
     * Reads N bytes from the input stream and writes them into this sink
     *
     * @param input
     * @param length
     * @throws IOException EOF if the source failed
     */
    void writeInputStream(InputStream input, long length) throws IOException;

    /**
     * Reads N bytes from the DataInput and writes them into this sink
     *
     * @param input
     * @param length
     * @throws IOException
     */
    void writeDataInput(DataInput input, long length) throws IOException;

}
