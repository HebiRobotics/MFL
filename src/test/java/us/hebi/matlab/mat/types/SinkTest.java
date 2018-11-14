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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.matlab.mat.util.Bytes.*;

/**
 * @author Florian Enner
 * @since 31 Aug 2018
 */
public class SinkTest {

    private final int n = 8 * 1024 + 16; // slightly more than multiple of internal copy buffer
    private ByteBuffer bb1, bb2, bb3;
    private Sink sink1, sink2;
    private Random rnd = new Random(0);

    @Before
    public void initBuffers() {
        bb1 = ByteBuffer.allocate(n); // (1) individual writes
        bb2 = ByteBuffer.allocate(n); // (2) array writes
        bb3 = ByteBuffer.allocate(n); // (3) ground truth
        sink1 = Sinks.wrap(bb1);
        sink2 = Sinks.wrap(bb2);
    }

    public void checkArrayEquality() {
        assertArrayEquals("for loop", bb3.array(), bb1.array());
        assertArrayEquals("array write", bb3.array(), bb2.array());
    }

    @Test
    public void writeBytes() throws Exception {
        byte[] values = new byte[n];
        rnd.nextBytes(values);
        for (int i = 0; i < values.length; i++) {
            sink1.writeByte(values[i]);
        }
        sink2.writeBytes(values, 0, values.length);
        bb3.put(values);
        checkArrayEquality();
    }

    @Test
    public void writeShorts() throws Exception {
        short[] values = new short[n / SIZEOF_SHORT];
        for (int i = 0; i < values.length; i++) {
            values[i] = (short) rnd.nextInt();
            sink1.writeShort(values[i]);
        }
        sink2.writeShorts(values, 0, values.length);
        bb3.asShortBuffer().put(values);
        checkArrayEquality();
    }

    @Test
    public void writeInts() throws Exception {
        int[] values = new int[n / SIZEOF_INT];
        for (int i = 0; i < values.length; i++) {
            values[i] = rnd.nextInt();
            sink1.writeInt(values[i]);
        }
        sink2.writeInts(values, 0, values.length);
        bb3.asIntBuffer().put(values);
        checkArrayEquality();
    }

    @Test
    public void writeLongs() throws Exception {
        long[] values = new long[n / SIZEOF_LONG];
        for (int i = 0; i < values.length; i++) {
            values[i] = rnd.nextLong();
            sink1.writeLong(values[i]);
        }
        sink2.writeLongs(values, 0, values.length);
        bb3.asLongBuffer().put(values);
        checkArrayEquality();
    }

    @Test
    public void writeFloats() throws Exception {
        float[] values = new float[n / SIZEOF_FLOAT];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) rnd.nextInt();
            sink1.writeFloat(values[i]);
        }
        sink2.writeFloats(values, 0, values.length);
        bb3.asFloatBuffer().put(values);
        checkArrayEquality();
    }

    @Test
    public void writeDoubles() throws Exception {
        double[] values = new double[n / SIZEOF_DOUBLE];
        for (int i = 0; i < values.length; i++) {
            values[i] = (double) rnd.nextInt();
            sink1.writeDouble(values[i]);
        }
        sink2.writeDoubles(values, 0, values.length);
        bb3.asDoubleBuffer().put(values);
        checkArrayEquality();
    }

    @Test
    public void writeByteBuffer() throws Exception {
        rnd.nextBytes(bb3.array());
        sink1.writeByteBuffer(bb3);
        assertArrayEquals(bb3.array(), bb1.array());
    }

    @Test
    public void writeInputStream() throws Exception {
        rnd.nextBytes(bb3.array());
        InputStream source = new ByteArrayInputStream(bb3.array());
        sink1.writeInputStream(source, bb3.remaining());
        assertArrayEquals(bb3.array(), bb1.array());
    }

    @Test
    public void writeDataInput() throws Exception {
        rnd.nextBytes(bb3.array());
        DataInput source = new DataInputStream(new ByteArrayInputStream(bb3.array()));
        sink1.writeDataInput(source, bb3.remaining());
        assertArrayEquals(bb3.array(), bb1.array());
    }

}
