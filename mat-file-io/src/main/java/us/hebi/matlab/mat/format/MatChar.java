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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.AbstractCharBase;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;

import static us.hebi.matlab.mat.util.Preconditions.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * Default char array implementation backed by a char buffer
 *
 * @author Florian Enner
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
    }

    @Override
    public int getMat5Size(String name) {
        buffer.rewind();
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + computeCharBufferSize(encoding, buffer);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        buffer.rewind();
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        writeCharBufferWithTag(encoding, buffer, sink);
    }

    protected final CharBuffer buffer;
    protected final CharEncoding encoding;

}
