/*-
 * #%L
 * Mat-File IO
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

/**
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public class ByteConverters {

    public static ByteConverter getSafest() {
        return heapConverter;
    }

    public static ByteConverter getFastest() {
        return getFastest(true);
    }

    public static ByteConverter getFastest(boolean checkBounds) {
        if (UnsafeAccess.isAvailable())
            return checkBounds ? unsafeConverterWithBoundsCheck : rawUnsafeConverter;
        return heapConverter;
    }

    /**
     * Native access that uses sun.misc.unsafe to read and write to arbitrary memory
     * locations such as the backing memory of native buffers and mapped files. This
     * may not perform any bounds checks and may segfault. Use with caution!
     *
     * @param atomic            makes reads/writes act like volatile variables
     * @param alwaysNativeOrder ignores the desired order and always assumes native order
     * @param baseOffset        base offset that gets added to any specified offset. Default=0
     */
    public static NativeAccess getNativeAccess(boolean atomic, boolean alwaysNativeOrder, long baseOffset) {
        UnsafeAccess.requireUnsafe();
        NativeAccess base = atomic ? new NativeAtomicAccess(baseOffset) : new NativeRegularAccess(baseOffset);
        return !alwaysNativeOrder ? new NativeOrderConverter(base) : base;
    }

    static {
        heapConverter = new HeapByteConverter();

        if (UnsafeAccess.isAvailable()) {
            final UnsafeByteConverter unsafeConverter = new UnsafeByteConverter();
            rawUnsafeConverter = unsafeConverter;
            unsafeConverterWithBoundsCheck = new CheckArrayBounds(unsafeConverter);
        } else {
            rawUnsafeConverter = null;
            unsafeConverterWithBoundsCheck = null;
        }

    }

    private static final ByteConverter heapConverter;
    private static final ByteConverter rawUnsafeConverter;
    private static final ByteConverter unsafeConverterWithBoundsCheck;

}
