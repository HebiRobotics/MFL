package us.hebi.matlab.io.types;

import us.hebi.matlab.common.util.Casts;

/**
 * Forwards convenience accessors to the minimum required set
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 Sep 2018
 */
public abstract class AbstractMatrixBase extends AbstractArray implements Matrix {

    protected AbstractMatrixBase(int[] dims, boolean isGlobal) {
        super(dims, isGlobal);
    }

    // ---- Internal convenience methods

    protected long orLogical(long value) {
        if (isLogical())
            return value != 0 ? 1 : 0;
        return value;
    }

    protected double orLogical(double value) {
        if (isLogical())
            return value != 0 ? 1 : 0;
        return value;
    }

    // ---- Index based methods that have to be implemented by child class

    @Override
    public abstract long getLong(int index);

    @Override
    public abstract void setLong(int index, long value);

    @Override
    public abstract double getDouble(int index);

    @Override
    public abstract void setDouble(int index, double value);

    @Override
    public abstract long getImaginaryLong(int index);

    @Override
    public abstract void setImaginaryLong(int index, long value);

    @Override
    public abstract double getImaginaryDouble(int index);

    @Override
    public abstract void setImaginaryDouble(int index, double value);

    // ---- Index based methods that may be implemented by child classes

    @Override
    public boolean getBoolean(int index) {
        return Casts.logical(getDouble(index));
    }

    @Override
    public byte getByte(int index) {
        return Casts.int8(getLong(index));
    }

    @Override
    public short getShort(int index) {
        return Casts.int16(getLong(index));
    }

    @Override
    public int getInt(int index) {
        return Casts.int32(getLong(index));
    }

    @Override
    public float getFloat(int index) {
        return Casts.single(getDouble(index));
    }

    @Override
    public void setBoolean(int index, boolean value) {
        setLong(index, Casts.int8(value));
    }

    @Override
    public void setByte(int index, byte value) {
        setLong(index, value);
    }

    @Override
    public void setShort(int index, short value) {
        setLong(index, value);
    }

    @Override
    public void setInt(int index, int value) {
        setLong(index, value);
    }

    @Override
    public void setFloat(int index, float value) {
        setDouble(index, value);
    }

    @Override
    public byte getImaginaryByte(int index) {
        return Casts.int8(getImaginaryLong(index));
    }

    @Override
    public short getImaginaryShort(int index) {
        return Casts.int16(getImaginaryLong(index));
    }

    @Override
    public int getImaginaryInt(int index) {
        return Casts.int32(getImaginaryLong(index));
    }

    @Override
    public float getImaginaryFloat(int index) {
        return Casts.single(getImaginaryDouble(index));
    }

    @Override
    public void setImaginaryByte(int index, byte value) {
        setImaginaryLong(index, value);
    }

    @Override
    public void setImaginaryShort(int index, short value) {
        setImaginaryLong(index, value);
    }

    @Override
    public void setImaginaryInt(int index, int value) {
        setImaginaryLong(index, value);
    }

    @Override
    public void setImaginaryFloat(int index, float value) {
        setImaginaryDouble(index, value);
    }

    // ---- Convenience overloads

    @Override
    public final boolean getBoolean(int row, int col) {
        return getBoolean(getColumnMajorIndex(row, col));
    }

    @Override
    public final byte getByte(int row, int col) {
        return getByte(getColumnMajorIndex(row, col));
    }

    @Override
    public final short getShort(int row, int col) {
        return getShort(getColumnMajorIndex(row, col));
    }

    @Override
    public final int getInt(int row, int col) {
        return getInt(getColumnMajorIndex(row, col));
    }

    @Override
    public final long getLong(int row, int col) {
        return getLong(getColumnMajorIndex(row, col));
    }

    @Override
    public final float getFloat(int row, int col) {
        return getFloat(getColumnMajorIndex(row, col));
    }

    @Override
    public final double getDouble(int row, int col) {
        return getDouble(getColumnMajorIndex(row, col));
    }

    @Override
    public final void setBoolean(int row, int col, boolean value) {
        setBoolean(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setByte(int row, int col, byte value) {
        setByte(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setShort(int row, int col, short value) {
        setShort(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setInt(int row, int col, int value) {
        setInt(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setLong(int row, int col, long value) {
        setLong(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setFloat(int row, int col, float value) {
        setFloat(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setDouble(int row, int col, double value) {
        setDouble(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final boolean getBoolean(int[] indices) {
        return getBoolean(getColumnMajorIndex(indices));
    }

    @Override
    public final byte getByte(int[] indices) {
        return getByte(getColumnMajorIndex(indices));
    }

    @Override
    public final short getShort(int[] indices) {
        return getShort(getColumnMajorIndex(indices));
    }

    @Override
    public final int getInt(int[] indices) {
        return getInt(getColumnMajorIndex(indices));
    }

    @Override
    public final long getLong(int[] indices) {
        return getLong(getColumnMajorIndex(indices));
    }

    @Override
    public final float getFloat(int[] indices) {
        return getFloat(getColumnMajorIndex(indices));
    }

    @Override
    public final double getDouble(int[] indices) {
        return getDouble(getColumnMajorIndex(indices));
    }

    @Override
    public final void setBoolean(int[] indices, boolean value) {
        setBoolean(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setByte(int[] indices, byte value) {
        setByte(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setShort(int[] indices, short value) {
        setShort(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setInt(int[] indices, int value) {
        setInt(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setLong(int[] indices, long value) {
        setLong(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setFloat(int[] indices, float value) {
        setFloat(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setDouble(int[] indices, double value) {
        setDouble(getColumnMajorIndex(indices), value);
    }

    @Override
    public final byte getImaginaryByte(int row, int col) {
        return getImaginaryByte(getColumnMajorIndex(row, col));
    }

    @Override
    public final short getImaginaryShort(int row, int col) {
        return getImaginaryShort(getColumnMajorIndex(row, col));
    }

    @Override
    public final int getImaginaryInt(int row, int col) {
        return getImaginaryInt(getColumnMajorIndex(row, col));
    }

    @Override
    public final long getImaginaryLong(int row, int col) {
        return getImaginaryLong(getColumnMajorIndex(row, col));
    }

    @Override
    public final float getImaginaryFloat(int row, int col) {
        return getImaginaryFloat(getColumnMajorIndex(row, col));
    }

    @Override
    public final double getImaginaryDouble(int row, int col) {
        return getImaginaryDouble(getColumnMajorIndex(row, col));
    }

    @Override
    public final void setImaginaryByte(int row, int col, byte value) {
        setImaginaryByte(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setImaginaryShort(int row, int col, short value) {
        setImaginaryShort(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setImaginaryInt(int row, int col, int value) {
        setImaginaryInt(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setImaginaryLong(int row, int col, long value) {
        setImaginaryLong(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setImaginaryFloat(int row, int col, float value) {
        setImaginaryFloat(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final void setImaginaryDouble(int row, int col, double value) {
        setImaginaryDouble(getColumnMajorIndex(row, col), value);
    }

    @Override
    public final byte getImaginaryByte(int[] indices) {
        return getImaginaryByte(getColumnMajorIndex(indices));
    }

    @Override
    public final short getImaginaryShort(int[] indices) {
        return getImaginaryShort(getColumnMajorIndex(indices));
    }

    @Override
    public final int getImaginaryInt(int[] indices) {
        return getImaginaryInt(getColumnMajorIndex(indices));
    }

    @Override
    public final long getImaginaryLong(int[] indices) {
        return getImaginaryLong(getColumnMajorIndex(indices));
    }

    @Override
    public final float getImaginaryFloat(int[] indices) {
        return getImaginaryFloat(getColumnMajorIndex(indices));
    }

    @Override
    public final double getImaginaryDouble(int[] indices) {
        return getImaginaryDouble(getColumnMajorIndex(indices));
    }

    @Override
    public final void setImaginaryByte(int[] indices, byte value) {
        setImaginaryByte(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setImaginaryShort(int[] indices, short value) {
        setImaginaryShort(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setImaginaryInt(int[] indices, int value) {
        setImaginaryInt(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setImaginaryLong(int[] indices, long value) {
        setImaginaryLong(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setImaginaryFloat(int[] indices, float value) {
        setImaginaryFloat(getColumnMajorIndex(indices), value);
    }

    @Override
    public final void setImaginaryDouble(int[] indices, double value) {
        setImaginaryDouble(getColumnMajorIndex(indices), value);
    }

}
