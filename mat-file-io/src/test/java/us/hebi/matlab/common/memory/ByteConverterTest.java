package us.hebi.matlab.common.memory;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public class ByteConverterTest {

    @Test
    public void testSafestRead() {
        testReading(ByteConverters.getSafest());
    }

    @Test
    public void testSafestWrite() {
        testWriting(ByteConverters.getSafest());
    }

    @Test
    public void testSafestReadBounds() {
        testReadBounds(ByteConverters.getSafest());
    }

    @Test
    public void testSafestWriteBounds() {
        testWriteBounds(ByteConverters.getSafest());
    }

    @Test
    public void testFastestRead() {
        testReading(ByteConverters.getFastest());
    }

    @Test
    public void testFastestWrite() {
        testWriting(ByteConverters.getFastest());
    }

    @Test
    public void testFastestReadBounds() {
        testReadBounds(ByteConverters.getFastest());
    }

    @Test
    public void testFastestWriteBounds() {
        testWriteBounds(ByteConverters.getFastest());
    }

    private void testReading(ByteConverter converter) {
        for (ByteOrder order : orders) {

            buffer.clear();
            buffer.order(order);
            buffer.putShort(shortVal);
            buffer.putInt(intVal);
            buffer.putLong(longVal);
            buffer.putFloat(floatVal);
            buffer.putDouble(doubleVal);

            int offset = 0;
            assertEquals("short", shortVal, converter.getShort(order, bytes, offset));
            offset += SIZEOF_SHORT;
            assertEquals("int", intVal, converter.getInt(order, bytes, offset));
            offset += SIZEOF_INT;
            assertEquals("long", longVal, converter.getLong(order, bytes, offset));
            offset += SIZEOF_LONG;
            assertEquals("float", floatVal, converter.getFloat(order, bytes, offset), 0);
            offset += SIZEOF_FLOAT;
            assertEquals("double", doubleVal, converter.getDouble(order, bytes, offset), 0);
        }
    }

    private void testWriting(ByteConverter converter) {
        for (ByteOrder order : orders) {
            int offset = 0;
            converter.putShort(shortVal, order, bytes, offset);
            offset += SIZEOF_SHORT;
            converter.putInt(intVal, order, bytes, offset);
            offset += SIZEOF_INT;
            converter.putLong(longVal, order, bytes, offset);
            offset += SIZEOF_LONG;
            converter.putFloat(floatVal, order, bytes, offset);
            offset += SIZEOF_FLOAT;
            converter.putDouble(doubleVal, order, bytes, offset);

            buffer.clear();
            buffer.order(order);
            assertEquals("short", shortVal, buffer.getShort());
            assertEquals("int", intVal, buffer.getInt());
            assertEquals("long", longVal, buffer.getLong());
            assertEquals("float", floatVal, buffer.getFloat(), 0);
            assertEquals("double", doubleVal, buffer.getDouble(), 0);

        }
    }

    private void testReadBounds(ByteConverter converter) {
        byte[] bytes = new byte[8];
        for (ByteOrder order : orders) {

            // Read at beginning of array
            converter.getShort(order, bytes, 0);
            converter.getInt(order, bytes, 0);
            converter.getLong(order, bytes, 0);
            converter.getFloat(order, bytes, 0);
            converter.getDouble(order, bytes, 0);

            // Read at the end of array
            converter.getShort(order, bytes, 6);
            converter.getInt(order, bytes, 4);
            converter.getLong(order, bytes, 0);
            converter.getFloat(order, bytes, 4);
            converter.getDouble(order, bytes, 0);

            // Read before array start
            try {
                converter.getShort(order, bytes, -1);
                fail("short");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.getInt(order, bytes, -1);
                fail("int");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.getLong(order, bytes, -1);
                fail("long");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.getFloat(order, bytes, -1);
                fail("float");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.getDouble(order, bytes, -1);
                fail("double");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            // Read after array end
            try {
                converter.getShort(order, bytes, 7);
                fail("short");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.getInt(order, bytes, 5);
                fail("int");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.getLong(order, bytes, 1);
                fail("long");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.getFloat(order, bytes, 5);
                fail("float");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.getDouble(order, bytes, 1);
                fail("double");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

        }
    }

    private void testWriteBounds(ByteConverter converter) {

        byte[] bytes = new byte[8];
        for (ByteOrder order : orders) {

            // Read at beginning of array
            converter.putShort(shortVal, order, bytes, 0);
            converter.putInt(intVal, order, bytes, 0);
            converter.putLong(longVal, order, bytes, 0);
            converter.putFloat(floatVal, order, bytes, 0);
            converter.putDouble(doubleVal, order, bytes, 0);

            // Read at the end of array
            converter.putShort(shortVal, order, bytes, 6);
            converter.putInt(intVal, order, bytes, 4);
            converter.putLong(longVal, order, bytes, 0);
            converter.putFloat(floatVal, order, bytes, 4);
            converter.putDouble(doubleVal, order, bytes, 0);

            // Read before array start
            try {
                converter.putShort(shortVal, order, bytes, -1);
                fail("short");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.putInt(intVal, order, bytes, -1);
                fail("int");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.putLong(longVal, order, bytes, -1);
                fail("long");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.putFloat(floatVal, order, bytes, -1);
                fail("float");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
            try {
                converter.putDouble(doubleVal, order, bytes, -1);
                fail("double");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            // Read after array end
            try {
                converter.putShort(shortVal, order, bytes, 7);
                fail("short");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.putInt(intVal, order, bytes, 5);
                fail("int");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.putLong(longVal, order, bytes, 1);
                fail("long");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.putFloat(floatVal, order, bytes, 5);
                fail("float");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }

            try {
                converter.putDouble(doubleVal, order, bytes, 1);
                fail("double");
            } catch (ArrayIndexOutOfBoundsException bounds) {
            }
        }
    }


    final ByteBuffer buffer = ByteBuffer.allocate(128);
    final byte[] bytes = buffer.array();

    static final ByteOrder[] orders = new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN};
    static final short shortVal = (short) 0x3FA2;
    static final int intVal = 0x83FAA28B;
    static final long longVal = 0x82FAB28783FAA28BL;
    static final float floatVal = 42525.319320f;
    static final double doubleVal = 139203414214.3209313912;

}