package us.hebi.matlab.common.memory;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author Florian Enner
 * @since 31 Aug 2018
 */
class UnsafeAccess {

    public static void requireUnsafe() {
        // throws an exception if not available
        if (!isAvailable())
            throw new AssertionError("Unsafe is not available on this platform");
    }

    public static boolean isAvailable() {
        return UNSAFE != null;
    }

    static {
        Unsafe unsafe = null;
        long baseOffset = 0;
        try {
            final PrivilegedExceptionAction<Unsafe> action =
                    new PrivilegedExceptionAction<Unsafe>() {
                        @Override
                        public Unsafe run() throws Exception {
                            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                            f.setAccessible(true);

                            return (Unsafe) f.get(null);
                        }
                    };

            unsafe = AccessController.doPrivileged(action);
            baseOffset = unsafe.arrayBaseOffset(byte[].class);
        } catch (final Exception ex) {
            // Not available
        }

        UNSAFE = unsafe;
        BYTE_ARRAY_OFFSET = baseOffset;
    }

    final static ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    final static Unsafe UNSAFE;
    final static long BYTE_ARRAY_OFFSET;

}
