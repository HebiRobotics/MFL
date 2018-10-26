package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.memory.Bytes;
import us.hebi.matlab.common.util.Charsets;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.io.types.Source;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import static java.nio.ByteOrder.*;
import static us.hebi.matlab.common.memory.Bytes.*;
import static us.hebi.matlab.common.util.Casts.*;
import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * Available character encodings for character data in MAT files.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
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
        encoderLE = newEncoder(charsetLE);
        encoderBE = newEncoder(charsetBE);
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

    CharBuffer readCharBuffer(Source source, int numBytes) throws IOException {

        if (this == UInt16) {

            // UInt16 encoded ascii (read individually)
            int numElements = checkedDivide(numBytes, SIZEOF_SHORT);
            CharBuffer charBuffer = CharBuffer.allocate(numElements);
            for (int i = 0; i < numElements; i++) {
                charBuffer.append((char) source.readShort());
            }
            charBuffer.rewind();
            return charBuffer;

        } else {

            // UTF encoded bytes (copy byte-wise)
            BufferAllocator bufferAllocator = Mat5.getDefaultBufferAllocator();
            ByteBuffer tmpBuffer = bufferAllocator.allocate(numBytes);
            try {
                // Read data into temporary buffer
                source.readByteBuffer(tmpBuffer);
                tmpBuffer.rewind();

                // Convert to char buffer
                return getCharset(source.order()).decode(tmpBuffer);

            } finally {
                // Release temporary buffer
                bufferAllocator.release(tmpBuffer);
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
        final CharsetEncoder encoder = getEncoder(order);
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

    private CharsetEncoder getEncoder(ByteOrder order) {
        CharsetEncoder encoder = order == BIG_ENDIAN ? encoderBE : encoderLE;
        if (encoder == null)
            throw new AssertionError("Charset '" + this + "' is not supported on this platform");
        return encoder;
    }

    private static CharsetEncoder newEncoder(Charset charset) {
        if (charset == null) return null;
        return charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private final Charset charsetLE;
    private final Charset charsetBE;
    private final CharsetEncoder encoderLE;
    private final CharsetEncoder encoderBE;
    private static final ThreadLocal<ByteBuffer> buffer = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(8 * 1024);
        }
    };

}
