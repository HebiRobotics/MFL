package us.hebi.matlab.common.memory;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 27 Aug 2018
 */
public class BytesTest {

    @Test
    public void nextPowerOfTwo() {
        assertEquals(1, Bytes.nextPowerOfTwo(0));
        assertEquals(8, Bytes.nextPowerOfTwo(5));
        assertEquals(8, Bytes.nextPowerOfTwo(8));
        assertEquals(4, Bytes.nextPowerOfTwo(4));
        assertEquals(1024, Bytes.nextPowerOfTwo(513));
        assertEquals(256, Bytes.nextPowerOfTwo(256));
    }

}