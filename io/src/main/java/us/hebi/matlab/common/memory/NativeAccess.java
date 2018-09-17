package us.hebi.matlab.common.memory;

import java.nio.ByteOrder;

/**
 * Native access that uses Unsafe to read and write to arbitrary memory
 * locations such as the backing memory of native buffers and mapped files.
 * This may not perform any bounds checks and may segfault. Use with caution!
 * <p>
 * Notes:
 * * May not be available on some platforms
 * * Accesses memory location at [start of object] + [offset]
 * * Specifying a null object treats the offset as the raw memory location
 * <p>
 * Reads and writes act equivalent to
 * <p>
 * sun.misc.Unsafe.putInt(Object object, long offset);
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 28 Aug 2018
 */
public interface NativeAccess {

    byte getByte(Object object, long offset);

    short getShort(ByteOrder order, Object object, long offset);

    int getInt(ByteOrder order, Object object, long offset);

    long getLong(ByteOrder order, Object object, long offset);

    float getFloat(ByteOrder order, Object object, long offset);

    double getDouble(ByteOrder order, Object object, long offset);

    void putByte(byte value, Object object, long offset);

    void putShort(short value, ByteOrder order, Object object, long offset);

    void putInt(int value, ByteOrder order, Object object, long offset);

    void putLong(long value, ByteOrder order, Object object, long offset);

    void putFloat(float value, ByteOrder order, Object object, long offset);

    void putDouble(double value, ByteOrder order, Object object, long offset);

}
