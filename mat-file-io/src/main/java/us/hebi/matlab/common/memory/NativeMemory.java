package us.hebi.matlab.common.memory;

import us.hebi.matlab.common.util.PlatformInfo;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * Utilities for working with native memory such as freeing the memory
 * backing direct ByteBuffers.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 31 Aug 2018
 */
public class NativeMemory {

    /**
     * Releases the native memory backing direct buffers. Normally, native memory
     * is released when the garbage collector collects the referencing Java Object.
     * However, in some cases such as when closing memory mapped files or
     * in environments that have such low allocation rates that the GC never
     * kicks in, it is necessary to free the memory manually.
     * <p>
     * Be extremely careful when using this! Subsequent calls to a direct buffer
     * will cause a segfault that crashes the runtime without an Exception or
     * way of recovery. Thus, only use for buffers that are internally managed
     * and that users never get a reference to.
     * <p>
     * In Java 8 and below this requires internal APIs that got removed in Java 9.
     * Java 9 later added a method to Unsafe to restore this functionality.
     * See https://bugs.openjdk.java.net/browse/JDK-8171377
     *
     * @param buffer the direct buffer that should be released
     * @throws IllegalArgumentException if the buffer is not a direct buffer
     */
    public static void freeDirectBuffer(ByteBuffer buffer) {
        checkArgument(buffer.isDirect(), "Buffer is not a direct buffer");
        cleaner.freeDirectBuffer(buffer);
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
                throw new AssertionError("Java 6 Cleaner failed to free DirectBuffer", e);
            }
        }

        Java6Cleaner() {
            try {
                getCleanerMethod = Class.forName("sun.nio.ch.DirectBuffer").getMethod("cleaner");
                cleanMethod = Class.forName("sun.misc.Cleaner").getMethod("clean");
            } catch (Exception e) {
                throw new AssertionError("Java 6 Cleaner not available", e);
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
                throw new AssertionError("Java 9 Cleaner failed to free DirectBuffer", e);
            }
        }

        Java9Cleaner() {
            try {
                INVOKE_CLEANER = UnsafeAccess.UNSAFE.getClass().getMethod("invokeCleaner", ByteBuffer.class);
            } catch (Exception e) {
                throw new AssertionError("Java 9 Cleaner not available", e);
            }
        }

        final Method INVOKE_CLEANER;

    }

}
