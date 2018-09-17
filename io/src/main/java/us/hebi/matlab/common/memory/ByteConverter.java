package us.hebi.matlab.common.memory;

import java.nio.ByteOrder;

/**
 * Converts bytes inside a raw byte array to appropriate primitive values.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 26 Aug 2018
 */
public interface ByteConverter {

    short getShort(ByteOrder order, byte[] bytes, int offset);

    int getInt(ByteOrder order, byte[] bytes, int offset);

    long getLong(ByteOrder order, byte[] bytes, int offset);

    float getFloat(ByteOrder order, byte[] bytes, int offset);

    double getDouble(ByteOrder order, byte[] bytes, int offset);

    void putShort(short value, ByteOrder order, byte[] bytes, int offset);

    void putInt(int value, ByteOrder order, byte[] bytes, int offset);

    void putLong(long value, ByteOrder order, byte[] bytes, int offset);

    void putFloat(float value, ByteOrder order, byte[] bytes, int offset);

    void putDouble(double value, ByteOrder order, byte[] bytes, int offset);

}
