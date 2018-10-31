package us.hebi.matlab.common.util;

/**
 * Static utility methods for dealing with cast functionality in
 * MATLAB. Note that these methods don't exactly conform to MATLAB's
 * behavior, i.e., MATLAB does a saturated cast to the max range.
 * <p>
 * That means that all negative values (top bit set) get converted
 * to zero, and that all values above get converted to the max value.
 * <p>
 * That behavior seems weird for the Java world. Thus,
 *
 * unsigned casts:
 * cast to the next larger integer, e.g., uint8becomes short.
 * uint64 are expressed as long.
 *
 * signed casts:
 * perform a range check and fail if the value is out of range
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public final class Casts {

    private Casts() {
    }

    public static boolean logical(double value) {
        return value != 0; // 0.23 in MATLAB is considered true
    }

    public static byte int8(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    public static byte int8(long value) {
        return (byte) value;
    }

    public static short int16(long value) {
        return (short) value;
    }

    public static int int32(long value) {
        return (int) value;
    }

    public static byte sint8(long value) {
        if (value > Byte.MAX_VALUE)
            throw new IllegalArgumentException("Value is above signed int8 range");
        if (value < Byte.MIN_VALUE)
            throw new IllegalArgumentException("Value is below signed int8 range");
        return (byte) value;
    }

    public static short sint16(long value) {
        if (value > Short.MAX_VALUE)
            throw new IllegalArgumentException("Value is above signed int16 range");
        if (value < Short.MIN_VALUE)
            throw new IllegalArgumentException("Value is below signed int16 range");
        return (short) value;
    }

    public static int sint32(long value) {
        if (value > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Value is above signed int32 range");
        if (value < Integer.MIN_VALUE)
            throw new IllegalArgumentException("Value is below signed int32 range");
        return (int) value;
    }

    public static float single(double value) {
        return (float) value;
    }

    public static short uint8(byte value) {
        return (short) (value & MASK_UINT8);
    }

    public static int uint16(short value) {
        return (value & MASK_UINT16);
    }

    public static long uint32(int value) {
        return (value & MASK_UINT32);
    }

    public static boolean isInteger(double value) {
        return Math.rint(value) == value && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static boolean fitsByte(long value) {
        return value == (byte) value || value >>> 8 == 0;
    }

    public static boolean fitsShort(long value) {
        return value == (short) value || value >>> 16 == 0;
    }

    public static boolean fitsInt(long value) {
        return value == (int) value || value >>> 32 == 0;
    }

    /**
     * Performs an integer division and makes sure that no remainder gets thrown away
     *
     * @param numerator numerator
     * @param denominator denominator
     * @return numerator / denominator
     * @throws IllegalArgumentException if there is a remainder
     */
    public static int checkedDivide(int numerator, int denominator) {
        int remainder = numerator % denominator;
        if (remainder != 0) {
            String format = String.format("%d is not a multiple of %d", numerator, denominator);
            throw new IllegalArgumentException(format);
        }
        return numerator / denominator;
    }


    public static final short MASK_UINT8 = 0xFF;
    public static final int MASK_UINT16 = 0xFFFF;
    public static final long MASK_UINT32 = 0xFFFFFFFFL;

}
