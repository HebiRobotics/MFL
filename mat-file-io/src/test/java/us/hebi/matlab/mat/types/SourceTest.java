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

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * @author Florian Enner
 * @since 14 Sep 2018
 */
public class SourceTest {

    private final int n = 8 * 1024 + 16; // slightly more than multiple of internal copy buffer
    private ByteBuffer bb = ByteBuffer.allocate(n);
    private Random rnd = new Random(0);

    @Before
    public void initBuffers() {
        bb.rewind();
        rnd.nextBytes(bb.array());
    }

    @Test
    public void readBytes() throws Exception {
        byte[] actual = new byte[n];
        byte[] expected = actual.clone();
        asSource(bb).readBytes(actual, 0, actual.length);
        bb.get(expected);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readShorts() throws Exception {
        short[] actual = new short[n / SIZEOF_SHORT];
        short[] expected = actual.clone();
        asSource(bb).readShorts(actual, 0, actual.length);
        bb.asShortBuffer().get(expected);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readInts() throws Exception {
        int[] actual = new int[n / SIZEOF_INT];
        int[] expected = actual.clone();
        asSource(bb).readInts(actual, 0, actual.length);
        bb.asIntBuffer().get(expected);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readLongs() throws Exception {
        long[] actual = new long[n / SIZEOF_LONG];
        long[] expected = actual.clone();
        asSource(bb).readLongs(actual, 0, actual.length);
        bb.asLongBuffer().get(expected);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readFloats() throws Exception {
        float[] actual = new float[n / SIZEOF_FLOAT];
        float[] expected = actual.clone();
        asSource(bb).readFloats(actual, 0, actual.length);
        bb.asFloatBuffer().get(expected);
        assertArrayEquals(expected, actual, 0);
    }

    @Test
    public void readDoubles() throws Exception {
        double[] actual = new double[n / SIZEOF_DOUBLE];
        double[] expected = actual.clone();
        asSource(bb).readDoubles(actual, 0, actual.length);
        bb.asDoubleBuffer().get(expected);
        assertArrayEquals(expected, actual, 0);
    }

    @Test
    public void readByteBuffer() throws Exception {
        ByteBuffer actual = ByteBuffer.allocate(n);
        asSource(bb).readByteBuffer(actual);
        assertArrayEquals(bb.array(), actual.array());
    }

    private Source asSource(ByteBuffer bb) {
        return Sources.wrap(bb.duplicate()).order(bb.order());
    }

}
