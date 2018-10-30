package us.hebi.matlab.io.types;

import us.hebi.matlab.common.memory.NativeMemory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static us.hebi.matlab.common.util.Preconditions.*;

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
        checkNotNull(file);
        checkArgument(file.exists() && file.isFile(), "must be an existing file");

        // File is larger than the max capacity (2 GB) of a buffer, so we use an InputStream instead.
        // Unfortunately, this limits us to read a very large file using a single thread :( At some point
        // we could build more complex logic that can map to more than a single buffer.
        if (file.length() > Integer.MAX_VALUE) {
            return wrapInputStream(new FileInputStream(file));
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
                NativeMemory.freeDirectBuffer(buffer);
                channel.close();
            }
        };

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

    private static class ByteBufferSource extends AbstractSource {

        private ByteBufferSource(ByteBuffer buffer, int bufferSize) {
            super(bufferSize);
            this.buffer = buffer;
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
