package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.AbstractCharBase;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.common.memory.Resources;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * Default char array implementation backed by a char buffer
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
class MatChar extends AbstractCharBase implements Mat5Serializable {

    MatChar(int[] dims, CharEncoding encoding) {
        this(dims, false, encoding, CharBuffer.allocate(getNumElements(dims)));
        Arrays.fill(buffer.array(), ' ');
    }

    MatChar(int[] dims, boolean global, CharEncoding encoding, CharBuffer buffer) {
        super(dims, global);
        checkArgument(buffer.remaining() == getNumElements(), "Unexpected number of elements");
        this.buffer = checkNotNull(buffer);
        this.encoding = checkNotNull(encoding);
    }

    @Override
    public CharSequence asCharSequence() {
        return buffer.slice();
    }

    @Override
    public char getChar(int index) {
        return buffer.get(index);
    }

    @Override
    public void setChar(int index, char value) {
        buffer.put(index, value);
    }

    @Override
    public void close() {
        Resources.release(buffer);
    }

    @Override
    public int getMat5Size(String name) {
        buffer.rewind();
        return Mat5.MATRIX_TAG_SIZE
                + Mat5Writer.computeArrayHeaderSize(name, this)
                + Mat5Writer.computeCharBufferSize(encoding, buffer);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        buffer.rewind();
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        Mat5Writer.writeCharBufferWithTag(encoding, buffer, sink);
    }

    protected final CharBuffer buffer;
    protected final CharEncoding encoding;

}
