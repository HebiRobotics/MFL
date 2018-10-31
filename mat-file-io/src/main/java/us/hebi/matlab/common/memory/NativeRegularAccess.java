package us.hebi.matlab.common.memory;

import java.nio.ByteOrder;

import static us.hebi.matlab.common.memory.UnsafeAccess.*;

/**
 * @author Florian Enner
 * @since 28 Aug 2018
 */
class NativeRegularAccess implements NativeAccess {

    @Override
    public byte getByte(Object object, long offset) {
        return UNSAFE.getByte(object, baseOffset + offset);
    }

    @Override
    public short getShort(ByteOrder order, Object object, long offset) {
        return UNSAFE.getShort(object, baseOffset + offset);
    }

    @Override
    public int getInt(ByteOrder order, Object object, long offset) {
        return UNSAFE.getInt(object, baseOffset + offset);
    }

    @Override
    public long getLong(ByteOrder order, Object object, long offset) {
        return UNSAFE.getLong(object, baseOffset + offset);
    }

    @Override
    public float getFloat(ByteOrder order, Object object, long offset) {
        return UNSAFE.getFloat(object, baseOffset + offset);
    }

    @Override
    public double getDouble(ByteOrder order, Object object, long offset) {
        return UNSAFE.getDouble(object, baseOffset + offset);
    }

    @Override
    public void putByte(byte value, Object object, long offset) {
        UNSAFE.putByte(object, baseOffset + offset, value);
    }

    @Override
    public void putShort(short value, ByteOrder order, Object object, long offset) {
        UNSAFE.putShort(object, baseOffset + offset, value);
    }

    @Override
    public void putInt(int value, ByteOrder order, Object object, long offset) {
        UNSAFE.putInt(object, baseOffset + offset, value);
    }

    @Override
    public void putLong(long value, ByteOrder order, Object object, long offset) {
        UNSAFE.putLong(object, baseOffset + offset, value);
    }

    @Override
    public void putFloat(float value, ByteOrder order, Object object, long offset) {
        UNSAFE.putFloat(object, baseOffset + offset, value);
    }

    @Override
    public void putDouble(double value, ByteOrder order, Object object, long offset) {
        UNSAFE.putDouble(object, baseOffset + offset, value);
    }

    NativeRegularAccess() {
        this(BYTE_ARRAY_OFFSET);
    }

    NativeRegularAccess(long baseOffset) {
        UnsafeAccess.requireUnsafe();
        this.baseOffset = baseOffset;
    }

    final long baseOffset;

}
