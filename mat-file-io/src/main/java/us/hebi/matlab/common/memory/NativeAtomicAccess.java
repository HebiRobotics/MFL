package us.hebi.matlab.common.memory;

import java.nio.ByteOrder;

import static us.hebi.matlab.common.memory.UnsafeAccess.*;

/**
 * @author Florian Enner
 * @since 28 Aug 2018
 */
class NativeAtomicAccess implements NativeAccess {

    @Override
    public byte getByte(Object object, long offset) {
        return UNSAFE.getByteVolatile(object, baseOffset + offset);
    }

    @Override
    public short getShort(ByteOrder order, Object object, long offset) {
        return UNSAFE.getShortVolatile(object, baseOffset + offset);
    }

    @Override
    public int getInt(ByteOrder order, Object object, long offset) {
        return UNSAFE.getIntVolatile(object, baseOffset + offset);
    }

    @Override
    public long getLong(ByteOrder order, Object object, long offset) {
        return UNSAFE.getLongVolatile(object, baseOffset + offset);
    }

    @Override
    public float getFloat(ByteOrder order, Object object, long offset) {
        return UNSAFE.getFloatVolatile(object, baseOffset + offset);
    }

    @Override
    public double getDouble(ByteOrder order, Object object, long offset) {
        return UNSAFE.getDoubleVolatile(object, baseOffset + offset);
    }

    @Override
    public void putByte(byte value, Object object, long offset) {
        UNSAFE.putByteVolatile(object, baseOffset + offset, value);
    }

    @Override
    public void putShort(short value, ByteOrder order, Object object, long offset) {
        UNSAFE.putShortVolatile(object, baseOffset + offset, value);
    }

    @Override
    public void putInt(int value, ByteOrder order, Object object, long offset) {
        UNSAFE.putIntVolatile(object, baseOffset + offset, value);
    }

    @Override
    public void putLong(long value, ByteOrder order, Object object, long offset) {
        UNSAFE.putLongVolatile(object, baseOffset + offset, value);
    }

    @Override
    public void putFloat(float value, ByteOrder order, Object object, long offset) {
        UNSAFE.putFloatVolatile(object, baseOffset + offset, value);
    }

    @Override
    public void putDouble(double value, ByteOrder order, Object object, long offset) {
        UNSAFE.putDoubleVolatile(object, baseOffset + offset, value);
    }

    NativeAtomicAccess() {
        this(BYTE_ARRAY_OFFSET);
    }

    NativeAtomicAccess(long baseOffset) {
        UnsafeAccess.requireUnsafe();
        this.baseOffset = baseOffset;
    }

    final long baseOffset;

}
