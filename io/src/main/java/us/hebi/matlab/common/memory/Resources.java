package us.hebi.matlab.common.memory;

import us.hebi.matlab.common.util.PlatformInfo;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Helps to free native memory of DirectBuffers. In Java 8 and below this
 * requires internal APIs that got removed in Java9.
 *
 * Java9 later added a method to Unsafe to restore the functionality.
 * See https://bugs.openjdk.java.net/browse/JDK-8171377
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 31 Aug 2018
 */
public class Resources {

    public static void release(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            cleaner.freeDirectBuffer(buffer);
        }
    }

    static {
        int version = PlatformInfo.getJavaVersion();

        if (version >= 9 && UnsafeAccess.isAvailable()) {
            cleaner = new Java9Cleaner();

        } else if (version < 9 && !PlatformInfo.isAndroid()) {
            cleaner = new Java6Cleaner();

        } else {
            cleaner = new DisabledCleaner();
        }
    }

    private static final Cleaner cleaner;

    private interface Cleaner {
        void freeDirectBuffer(ByteBuffer buffer);
    }

    private static class DisabledCleaner implements Cleaner {
        @Override
        public void freeDirectBuffer(ByteBuffer buffer) {
        }
    }

    /**
     * sun.nio.ch.DirectBuffer::cleaner()
     * sun.misc.Cleaner::freeDirectBuffer()
     */
    private static class Java6Cleaner implements Cleaner {
        @Override
        public void freeDirectBuffer(ByteBuffer buffer) {
            try {
                cleanMethod.invoke(getCleanerMethod.invoke(buffer));
            } catch (Exception e) {
                throw new AssertionError("Java6Cleaner failed to free DirectBuffer", e);
            }
        }

        Java6Cleaner() {
            try {
                getCleanerMethod = Class.forName("sun.nio.ch.DirectBuffer").getMethod("cleaner");
                cleanMethod = Class.forName("sun.misc.Cleaner").getMethod("clean");
            } catch (Exception e) {
                throw new AssertionError("Java6Cleaner not available", e);
            }
        }

        final Method getCleanerMethod;
        final Method cleanMethod;

    }

    /**
     * sun.misc.Unsafe::invokeCleaner(buffer)
     */
    private static class Java9Cleaner implements Cleaner {
        @Override
        public void freeDirectBuffer(ByteBuffer buffer) {
            try {
                INVOKE_CLEANER.invoke(UnsafeAccess.UNSAFE, buffer);
            } catch (Exception e) {
                throw new AssertionError("Java9Cleaner failed to free DirectBuffer", e);
            }
        }

        Java9Cleaner() {
            try {
                INVOKE_CLEANER = UnsafeAccess.UNSAFE.getClass().getMethod("invokeCleaner", ByteBuffer.class);
            } catch (Exception e) {
                throw new AssertionError("Java9Cleaner not available", e);
            }
        }

        final Method INVOKE_CLEANER;

    }

}
