/*-
 * #%L
 * Mat-File IO
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
import us.hebi.matlab.mat.util.Casts;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Factory for creating sinks for various types of output targets
 *
 * @author Florian Enner
 * @since 07 May 2018
 */
public class Sinks {

    /**
     * Creates a new file that continuously expands until it is closed. Supports
     * files that can become larger than 2 GB.
     *
     * @param file target file
     * @return sink writing to file
     * @throws IOException if file can't be opened
     */
    public static Sink newStreamingFile(final File file) throws IOException {
        checkFile(file);
        final FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        final ByteBuffer directBuffer = ByteBuffer.allocateDirect(8 * defaultCopyBufferSize);

        return new AbstractSink(defaultCopyBufferSize) {
            @Override
            public long position() throws IOException {
                return channel.position() + directBuffer.position();
            }

            @Override
            public void position(long position) throws IOException {
                flush();
                channel.position(position);
            }

            @Override
            public void writeByteBuffer(ByteBuffer buffer) throws IOException {
                flush();
                channel.write(buffer);
            }

            @Override
            public void writeBytes(byte[] buffer, int offset, int length) throws IOException {
                while (length > 0) {

                    // Copy to buffer
                    final int n = Math.min(length, directBuffer.remaining());
                    directBuffer.put(buffer, offset, n);
                    offset += n;
                    length -= n;

                    // Flush when necessary
                    if (directBuffer.remaining() == 0)
                        flush();

                }
            }

            private void flush() throws IOException {
                if (directBuffer.position() == 0)
                    return;
                directBuffer.flip();
                channel.write(directBuffer);
                directBuffer.clear();
            }

            @Override
            public void close() throws IOException {
                flush();
                channel.close();
                Unsafe9R.invokeCleaner(directBuffer);
            }
        };

    }

    public static Sink newStreamingFile(String file) throws IOException {
        return newStreamingFile(new File(file));
    }

    /**
     * Creates a memory mapped byte buffer with the maximum expected size that
     * truncates the backing file to the actual size once closed. Supports a
     * max file size of 2 GB.
     *
     * @param file            target file
     * @param maxExpectedSize initial size of the file
     * @return sink writing to file
     * @throws IOException if file can't be opened
     */
    public static Sink newMappedFile(File file, int maxExpectedSize) throws IOException {
        checkFile(file);

        // Memory map largest possible size
        final FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        final ByteBuffer buffer = channel
                .map(FileChannel.MapMode.READ_WRITE, 0, maxExpectedSize)
                .load();

        // Unmap buffer and truncate file to actual size
        return new BufferSink(buffer, defaultCopyBufferSize) {
            @Override
            public void close() throws IOException {
                super.close();
                long finalSize = buffer.position();
                Unsafe9R.invokeCleaner(buffer);
                channel.truncate(finalSize);
                channel.close();
            }
        };

    }

    public static Sink wrap(final ByteBuffer buffer) {
        return new BufferSink(buffer, Math.min(buffer.remaining(), defaultCopyBufferSize));
    }

    /**
     * Wraps an existing output stream. This does not support position seeking, which
     * may prohibit some functionality. Used internally for creating deflated sub-sinks.
     *
     * @param outputStream outputStream
     * @return sink writing to the output stream
     */
    public static Sink wrapNonSeeking(OutputStream outputStream) {
        return wrapNonSeeking(outputStream, defaultCopyBufferSize);
    }

    public static Sink wrapNonSeeking(OutputStream outputStream, int copyBufferSize) {
        return new OutputStreamSink(outputStream, copyBufferSize);
    }

    private static final int defaultCopyBufferSize = 4096;

    private static void checkFile(File file) throws IOException {
        if (checkNotNull(file).exists() && !file.delete())
            throw new IOException("Failed to overwrite " + file.getAbsolutePath());
        createParentDirs(file);
    }

    private static void createParentDirs(File file) throws IOException {
        File parent = file.getCanonicalFile().getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Unable to create parent directories of " + file);
        }
    }

    private static class OutputStreamSink extends AbstractSink {

        public long position() throws IOException {
            return position;
        }

        public void position(long position) throws IOException {
            throw new IllegalStateException("Sink does not implement position seeking");
        }

        @Override
        public void writeBytes(byte[] buffer, int offset, int length) throws IOException {
            output.write(buffer, offset, length);
            position += length;
        }

        protected OutputStreamSink(OutputStream output, int bufferSize) {
            super(bufferSize);
            this.output = output;
        }

        @Override
        public void close() throws IOException {
            output.close();
        }

        private long position = 0;
        final OutputStream output;

    }

    private static class BufferSink extends AbstractSink {

        private BufferSink(ByteBuffer out, int writeBufferSize) {
            super(writeBufferSize);
            this.out = out;
        }

        @Override
        public ByteOrder order() {
            return out.order();
        }

        @Override
        public AbstractSink order(ByteOrder order) {
            out.order(order);
            return this;
        }

        @Override
        public long position() {
            return out.position();
        }

        @Override
        public void position(long position) {
            out.position(Casts.sint32(position));
        }

        @Override
        public void writeByteBuffer(ByteBuffer src) {
            out.put(src);
        }

        @Override
        public void writeBytes(byte[] buffer, int offset, int length) {
            out.put(buffer, offset, length);
        }

        @Override
        public void close() throws IOException {

        }

        final ByteBuffer out;
    }

    static class SinkOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            sink.writeByte((byte) (b & 0xFF));
        }

        public void write(byte b[], int off, int len) throws IOException {
            sink.writeBytes(b, off, len);
        }

        SinkOutputStream(Sink sink) {
            this.sink = checkNotNull(sink);
        }

        private final Sink sink;

    }

    private Sinks() {
    }

}
