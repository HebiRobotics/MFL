package us.hebi.matlab.io.mat;

import java.nio.ByteBuffer;

/**
 * Interface for implementing custom memory management. Useful for working
 * with buffer pools or memory mapped files.
 *
 * @author Florian Enner
 * @since 28 Oct 2018
 */
public interface BufferAllocator {

    /**
     * Creates a buffer with the following properties:
     * <p>
     * - position() equal to 0
     * - limit() equal to numBytes
     * - capacity() larger or equal to numBytes
     * - All remaining bytes must be zero
     * <p>
     * This method must be thread-safe.
     *
     * @param numBytes number of bytes
     * @return zeroed buffer at position zero with numBytes remaining
     */
    ByteBuffer allocate(int numBytes);

    /**
     * Releases the buffer. This method gets called when the caller
     * gives up rights to use the buffer. The buffer itself may be
     * used again as part of a buffer pool.
     * <p>
     * This method must be thread-safe.
     *
     * @param buffer buffer
     */
    void release(ByteBuffer buffer);

}
