/*-
 * #%L
 * Glue
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.common.memory;

import us.hebi.matlab.common.util.PlatformInfo;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Provides reflective access to new methods that were added to sun.misc.Unsafe
 * in Java 9. Earlier platforms will fall back to a a behavior-equivalent
 * implementation that uses available operations.
 * <p>
 * ========== JDK Warning: sun.misc.Unsafe ==========
 * <p>
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * <em>Note:</em> It is the resposibility of the caller to make sure
 * arguments are checked before methods of this class are
 * called. While some rudimentary checks are performed on the input,
 * the checks are best effort and when performance is an overriding
 * priority, as when methods of this class are optimized by the
 * runtime compiler, some or all checks (if any) may be elided. Hence,
 * the caller must not rely on the checks and corresponding
 * exceptions!
 *
 * @author Florian Enner
 * @since 1.6
 */
public class Unsafe9R {

    /**
     * Invokes the given direct byte buffer's cleaner, if any.
     *
     * @param directBuffer a direct byte buffer
     * @throws NullPointerException     if {@code directBuffer} is null
     * @throws IllegalArgumentException if {@code directBuffer} is non-direct
     * @throws IllegalArgumentException if {@code directBuffer} is slice or duplicate (Java 9+ only)
     * @since 9
     */
    public static void invokeCleaner(ByteBuffer directBuffer) {
        if (!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is non-direct");
        cleaner.invokeCleaner(directBuffer);
    }

    static {

        // Get Java version
        String version = System.getProperty("java.specification.version", "6");
        String majorPart = version.startsWith("1.") ? version.substring(2) : version;
        int majorVersion = Integer.parseInt(majorPart);

        if (majorVersion >= 9) {
            cleaner = new Java9Cleaner();
        }else{
            cleaner = new Java6Cleaner();
        }

    }

    private static final Cleaner cleaner;

    private interface Cleaner {
        void invokeCleaner(ByteBuffer buffer);
    }

    /**
     * sun.nio.ch.DirectBuffer::cleaner()
     * sun.misc.Cleaner::invokeCleaner()
     */
    private static class Java6Cleaner implements Cleaner {
        @Override
        public void invokeCleaner(ByteBuffer buffer) {
            try {
                Object cleaner = GET_CLEANER.invoke(buffer);
                if (cleaner != null)
                    INVOKE_CLEANER.invoke(cleaner);
            } catch (Exception e) {
                throw new AssertionError("Java 6 Cleaner failed to free DirectBuffer", e);
            }
        }

        Java6Cleaner() {
            try {
                GET_CLEANER = Class.forName("sun.nio.ch.DirectBuffer").getMethod("cleaner");
                INVOKE_CLEANER = Class.forName("sun.misc.Cleaner").getMethod("clean");
            } catch (Exception e) {
                throw new AssertionError("Java 6 Cleaner not available", e);
            }
        }

        final Method GET_CLEANER;
        final Method INVOKE_CLEANER;

    }

    /**
     * sun.misc.Unsafe::invokeCleaner(buffer)
     */
    private static class Java9Cleaner implements Cleaner {
        @Override
        public void invokeCleaner(ByteBuffer buffer) {
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