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

package us.hebi.matlab.mat.types;

import us.hebi.matlab.mat.util.Unsafe9R;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static us.hebi.matlab.mat.types.Sinks.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Factory methods for creating sources from various underlying inputs
 *
 * @author Florian Enner
 * @since 03 May 2018
 */
public class Sources {

    public static Source openFile(String file) throws IOException {
        return openFile(new File((file)));
    }

    public static Source openFile(File file) throws IOException {
        checkArgument(checkNotNull(file).exists() && file.isFile(), "must be an existing file");

        // File is larger than the max capacity (2 GB) of a buffer, so we use an InputStream instead.
        // Unfortunately, this limits us to read a very large file using a single thread :( At some point
        // we could build more complex logic that can map to more than a single buffer.
        if (file.length() > Integer.MAX_VALUE) {
            return openStreamingFile(file);
        }

        // File is small enough to be memory-mapped into a single buffer
        final FileChannel channel = new RandomAccessFile(file, "r").getChannel();
        final ByteBuffer buffer = channel
                .map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size())
                .load();
        buffer.order(ByteOrder.nativeOrder());

        // Wrap as source
        return new ByteBufferSource(buffer, 512) {
            @Override
            public void close() throws IOException {
                super.close();
                Unsafe9R.invokeCleaner(buffer);
                channel.close();
            }
        };

    }

    public static Source openStreamingFile(File file) throws IOException {
        checkArgument(checkNotNull(file).exists() && file.isFile(), "must be an existing file");
        return new BufferedFileSource(file);
    }

    public static Source wrap(byte[] bytes) {
        return wrap(ByteBuffer.wrap(bytes));
    }

    public static Source wrap(ByteBuffer buffer) {
        return new ByteBufferSource(buffer, 128);
    }

    public static Source wrapInputStream(InputStream inputStream) {
        return wrapInputStream(inputStream, 512);
    }

    public static Source wrapInputStream(InputStream inputStream, int bufferSize) {
        return new InputStreamSource(checkNotNull(inputStream), bufferSize);
    }

    private static class BufferedFileSource extends AbstractSource {
        protected BufferedFileSource(File file) throws IOException {
            super(512);
            this.fileChannel = new RandomAccessFile(file, "rw").getChannel();
            this.directBuffer.limit(0); // don't pre-fetch data yet
        }

        @Override
        protected InputStream readBytesAsStream(long numBytes) throws IOException {
            return new SourceInputStream(this, numBytes);
        }

        @Override
        public long getPosition() {
            return fileChannelPosition + directBuffer.position();
        }

        @Override
        public void readByteBuffer(ByteBuffer buffer) throws IOException {
            // (1) Non-heap (non read-only) buffer -> copy via byte[] array
            if (buffer.hasArray()) {
                super.readByteBuffer(buffer);
                return;
            }

            // (2) Copy whatever data is available in the read buffer
            if (directBuffer.remaining() > 0) {

                // Lower limit to max what fits inside the buffer to prevent overflow
                int oldLimit = directBuffer.limit();
                int limit = Math.min(oldLimit, directBuffer.position() + buffer.remaining());

                // Reset limit after
                directBuffer.limit(limit);
                buffer.put(directBuffer);
                directBuffer.limit(oldLimit);
            }

            // (3) Copy directly in case more data is needed
            if (buffer.hasRemaining()) {
                checkState(!directBuffer.hasRemaining(), "Read buffer was not fully emptied");

                int numBytes = fileChannel.read(buffer);
                fileChannelPosition += Math.max(0, numBytes);
                if (buffer.hasRemaining())
                    throw new EOFException();
            }

        }

        @Override
        public void readBytes(final byte[] buffer, int offset, int length) throws IOException {
            while (length > 0) {

                // Make sure buffer isn't empty
                if (directBuffer.remaining() == 0) {
                    fileChannelPosition += directBuffer.position();
                    directBuffer.clear();
                    if (fileChannel.read(directBuffer) == -1)
                        throw new EOFException();
                    directBuffer.flip();
                }

                // Copy as many bytes as are available
                final int n = Math.min(directBuffer.remaining(), length);
                directBuffer.get(buffer, offset, n);
                offset += n;
                length -= n;

            }
        }

        @Override
        public boolean isMutatedByChildren() {
            return true;
        }

        @Override
        public void close() throws IOException {
            Unsafe9R.invokeCleaner(directBuffer);
            fileChannel.close();
        }

        private final ByteBuffer directBuffer = ByteBuffer.allocateDirect(DISK_IO_BUFFER_SIZE);
        private final FileChannel fileChannel;
        private long fileChannelPosition = 0;

    }

    private static class ByteBufferSource extends AbstractSource {

        private ByteBufferSource(ByteBuffer buffer, int bufferSize) {
            super(bufferSize);
            this.buffer = buffer;
        }

        @Override
        public AbstractSource order(ByteOrder byteOrder) {
            super.order(byteOrder);
            buffer.order(byteOrder);
            return this;
        }

        @Override
        public ByteOrder order() {
            return buffer.order();
        }

        @Override
        public byte readByte() throws IOException {
            try {
                return buffer.get();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public short readShort() throws IOException {
            try {
                return buffer.getShort();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public int readInt() throws IOException {
            try {
                return buffer.getInt();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public long readLong() throws IOException {
            try {
                return buffer.getLong();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public float readFloat() throws IOException {
            try {
                return buffer.getFloat();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public double readDouble() throws IOException {
            try {
                return buffer.getDouble();
            } catch (BufferUnderflowException underflow) {
                throw new EOFException();
            }
        }

        @Override
        public InputStream readBytesAsStream(long numBytes) throws IOException {
            if (numBytes > buffer.remaining())
                throw new EOFException();

            // Create buffer on subsection
            ByteBuffer slice = buffer.asReadOnlyBuffer();
            slice.limit(buffer.position() + (int) numBytes);

            // Move underlying buffer along
            buffer.position(buffer.position() + (int) numBytes);
            return new ByteBufferInputStream(slice);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isMutatedByChildren() {
            return false;
        }

        @Override
        public void readByteBuffer(ByteBuffer dst) throws IOException {
            if (dst.remaining() > buffer.remaining())
                throw new EOFException();

            // Set dummy limit as there is no bb.get(bb) method
            int limit = buffer.limit();
            try {
                buffer.limit(buffer.position() + dst.remaining());
                dst.put(buffer);
            } finally {
                buffer.limit(limit);
            }
        }

        @Override
        public void readBytes(byte[] bytes, int offset, int length) throws IOException {
            if (length > buffer.remaining())
                throw new EOFException();
            buffer.get(bytes, offset, length);
        }

        @Override
        public long getPosition() {
            return buffer.position();
        }

        final ByteBuffer buffer;

    }

    private static class ByteBufferInputStream extends InputStream {
        private ByteBufferInputStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            if (buffer.remaining() > 0)
                return buffer.get() & 0xFF;
            return -1;
        }

        @Override
        public int read(byte b[], int off, int len) {
            len = Math.min(len, buffer.remaining());
            if (len > 0) {
                buffer.get(b, off, len);
                return len;
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            super.close();
        }

        private final ByteBuffer buffer;
    }

    private static class InputStreamSource extends AbstractSource {

        InputStreamSource(InputStream input, int bufferSize) {
            super(bufferSize);
            this.input = input;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public void readBytes(byte[] bytes, int offset, int length) throws IOException {
            int n = 0;
            while (n < length) {
                int count = input.read(bytes, offset + n, length - n);
                if (count < 0) {
                    String format = "Reached end of stream after reading %d bytes. Expected %d bytes.";
                    throw new EOFException(String.format(format, n, length));
                }
                n += count;
            }
            position += length;
        }

        @Override
        public boolean isMutatedByChildren() {
            return true;
        }

        @Override
        protected InputStream readBytesAsStream(long numBytes) {
            return new SourceInputStream(this, numBytes);
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        long position = 0;
        final InputStream input;

    }

    private static class SourceInputStream extends InputStream {

        private SourceInputStream(Source matInput, long maxLength) {
            this.matInput = matInput;
            this.maxLength = maxLength;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int remaining = (int) (maxLength - position);
            if (remaining == 0)
                return -1;

            len = Math.min(len, remaining);
            matInput.readBytes(b, off, len);
            position += len;
            return len;
        }

        @Override
        public int read() {
            throw new IllegalStateException("not implemented");
        }

        long position = 0;
        final Source matInput;
        final long maxLength;

    }

    private Sources() {
    }

}
