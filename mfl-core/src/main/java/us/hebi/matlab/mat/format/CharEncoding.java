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

import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Source;
import us.hebi.matlab.mat.util.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.*;

import static java.nio.ByteOrder.*;
import static us.hebi.matlab.mat.util.Bytes.*;
import static us.hebi.matlab.mat.util.Casts.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Available character encodings for character data in MAT files.
 *
 * @author Florian Enner
 * @since 07 May 2018
 */
public enum CharEncoding {
    UInt16(Charsets.US_ASCII, Charsets.US_ASCII), // 2 byte per ascii character
    Utf8(Charsets.UTF_8, Charsets.UTF_8),
    Utf16(Charsets.UTF_16LE, Charsets.UTF_32BE),
    Utf32(Charsets.UTF_32LE, Charsets.UTF_32BE); // doesn't seem to actually work in MATLAB

    CharEncoding(Charset charsetLE, Charset charsetBE) {
        this.charsetLE = charsetLE;
        this.charsetBE = charsetBE;
    }

    static String parseAsciiString(byte[] buffer) {
        return parseAsciiString(buffer, 0, buffer.length);
    }

    static String parseAsciiString(byte[] buffer, int offset, int maxLength) {
        // Stop at String end character
        int length = Bytes.findFirst(buffer, offset, maxLength, (byte) '\0', maxLength);

        // Remove right-side trailing spaces
        while (length > 0 && buffer[length - 1] == ' ') {
            length--;
        }

        // Convert to String
        return length == 0 ? "" : new String(buffer, offset, length, Charsets.US_ASCII);
    }

    static class CloseableCharBuffer implements Closeable {

        static CloseableCharBuffer wrap(String value) {
            return new CloseableCharBuffer(CharBuffer.wrap(value), null, null);
        }

        static CloseableCharBuffer allocate(BufferAllocator bufferAllocator, int numChars) {
            ByteBuffer bytes = bufferAllocator.allocate(numChars * SIZEOF_CHAR);
            bytes.order(ByteOrder.nativeOrder());
            CharBuffer chars = bytes.asCharBuffer();
            checkState(chars.remaining() == numChars, "invalid buffer size");
            return new CloseableCharBuffer(chars, bytes, bufferAllocator);
        }

        static CloseableCharBuffer allocate(BufferAllocator bufferAllocator, int numChars, char fillChar) {
            return allocate(bufferAllocator, numChars).fill(fillChar);
        }

        CloseableCharBuffer fill(final char c) {
            for (int i = 0; i < chars.limit(); i++) {
                chars.put(i, c);
            }
            return this;
        }

        private CloseableCharBuffer(CharBuffer chars, ByteBuffer bytes, BufferAllocator bufferAllocator) {
            this.chars = chars;
            this.bytes = bytes;
            this.bufferAllocator = bufferAllocator;
        }

        @Override
        public void close() {
            if (bytes != null && bufferAllocator != null) {
                bufferAllocator.release(bytes);
            }
        }

        final CharBuffer chars;
        final ByteBuffer bytes;
        final BufferAllocator bufferAllocator;

    }

    CloseableCharBuffer readCharBuffer(Source source, int numBytes, BufferAllocator bufferAllocator) throws IOException {
        if (this == UInt16) {

            // UInt16 encoded ascii (map buffer directly)
            int numChars = checkedDivide(numBytes, SIZEOF_CHAR);
            CloseableCharBuffer buffer = CloseableCharBuffer.allocate(bufferAllocator, numChars);
            ByteBuffer bytes = buffer.bytes;
            bytes.order(source.order());
            source.readByteBuffer(bytes);
            checkState(!bytes.hasRemaining(), "read incorrect number of bytes");
            bytes.rewind();
            return buffer;

        } else {

            // UTF encoded bytes (copy byte-wise)
            ByteBuffer tmp = numBytes > TMP_BUFFER_SIZE ? bufferAllocator.allocate(numBytes) : buffer.get();
            try {
                // Read data into temporary buffer
                tmp.position(0);
                tmp.limit(numBytes);
                source.readByteBuffer(tmp);
                checkState(!tmp.hasRemaining(), "read incorrect number of bytes");
                tmp.rewind();

                // Count number of characters
                CharBuffer tmpChars = charBuffer.get();
                CharsetDecoder decoder = newDecoder(source.order());

                int numChars = 0;
                do {
                    tmpChars.clear();
                    CoderResult status = decoder.decode(tmp, tmpChars, true);
                    if (status.isError()) status.throwException();
                    numChars += tmpChars.position();
                }
                while (tmp.hasRemaining());

                // Allocate appropriately sized buffer
                CloseableCharBuffer buffer = CloseableCharBuffer.allocate(bufferAllocator, numChars);
                CharBuffer chars = buffer.chars;

                if (numChars == tmpChars.position()) {
                    // We already have all decoded chars -> copy
                    tmpChars.flip();
                    chars.put(tmpChars);
                } else {
                    // Decode all over again
                    tmp.rewind();
                    decoder.reset();
                    CoderResult status = decoder.decode(tmp, chars, true);
                    if (status.isError()) status.throwException();
                }

                checkState(!chars.hasRemaining(), "did not read expected number of characters");
                chars.flip();
                return buffer;

            } finally {
                // Release temporary buffer
                if (numBytes > TMP_BUFFER_SIZE) {
                    bufferAllocator.release(tmp);
                }
            }

        }
    }

    public int getEncodedLength(CharBuffer chars) {
        int position = chars.position();
        try {
            if (this == UInt16)
                return chars.remaining() * SIZEOF_SHORT;
            return writeEncodedUtf(chars, null);
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        } finally {
            chars.position(position);
        }
    }

    public void writeEncoded(CharBuffer chars, Sink sink) throws IOException {
        int position = chars.position();
        try {
            if (this == UInt16) {
                writeEncodedUInt16(chars, sink);
            } else {
                writeEncodedUtf(checkNotNull(chars), checkNotNull(sink));
            }
        } finally {
            chars.position(position);
        }
    }

    private void writeEncodedUInt16(CharBuffer chars, Sink sink) throws IOException {
        while (chars.hasRemaining()) {
            sink.writeShort((short) chars.get());
        }
    }

    private synchronized int writeEncodedUtf(CharBuffer chars, Sink sink) throws IOException {
        if (!chars.hasRemaining())
            return 0;

        // inspired by https://stackoverflow.com/a/8512572/3574093

        // reuse cached encoder. not thread-safe, so synchronized. If this
        // ever becomes a problem, we could do some thread-local magic.
        final ByteOrder order = (sink == null) ? ByteOrder.nativeOrder() : sink.order();
        final CharsetEncoder encoder = newEncoder(order);
        final ByteBuffer tmp = buffer.get();

        try {
            // encoding loop
            int length = 0;
            CoderResult status;
            do {
                tmp.clear();
                status = encoder.encode(chars, tmp, true);
                if (status.isError())
                    status.throwException();
                if (sink != null) {
                    tmp.flip();
                    sink.writeByteBuffer(tmp);
                }
                length += tmp.position();
            }
            while (status.isOverflow());

            // flush any remaining buffered state
            tmp.clear();
            status = encoder.flush(tmp);
            if (status.isError() || status.isOverflow())
                status.throwException();
            if (sink != null) {
                tmp.flip();
                sink.writeByteBuffer(tmp);
            }
            length += tmp.position();
            return length;

        } finally {
            // make sure we never leave the encoder in a bad state
            encoder.reset();
        }

    }

    private Charset getCharset(ByteOrder order) {
        Charset charset = order == BIG_ENDIAN ? charsetBE : charsetLE;
        if (charset == null)
            throw new AssertionError("Charset '" + this + "' is not supported on this platform");
        return charset;
    }

    private CharsetEncoder newEncoder(ByteOrder order) {
        return getCharset(order).newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private CharsetDecoder newDecoder(ByteOrder order) {
        return getCharset(order).newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private final Charset charsetLE;
    private final Charset charsetBE;
    private static final int TMP_BUFFER_SIZE = 8 * 1024;
    private static final ThreadLocal<ByteBuffer> buffer = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(TMP_BUFFER_SIZE);
        }
    };
    private static final ThreadLocal<CharBuffer> charBuffer = new ThreadLocal<CharBuffer>() {
        @Override
        protected CharBuffer initialValue() {
            return CharBuffer.allocate(TMP_BUFFER_SIZE / SIZEOF_CHAR);
        }
    };

}
