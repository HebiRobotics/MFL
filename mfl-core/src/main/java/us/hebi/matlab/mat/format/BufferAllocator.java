/*-
 * #%L
 * MAT File Library
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.mat.format;

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
