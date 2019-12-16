/*-
 * #%L
 * MAT File Library
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

package us.hebi.matlab.mat.util;

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

    static {
        heapConverter = new HeapByteConverter();

        if (UnsafeAccess.isAvailable()) {
            final UnsafeByteConverter unsafeConverter = new UnsafeByteConverter();
            rawUnsafeConverter = unsafeConverter;
            unsafeConverterWithBoundsCheck = new ArrayBoundsCheck(unsafeConverter);
        } else {
            rawUnsafeConverter = null;
            unsafeConverterWithBoundsCheck = null;
        }

    }

    private static final ByteConverter heapConverter;
    private static final ByteConverter rawUnsafeConverter;
    private static final ByteConverter unsafeConverterWithBoundsCheck;

}
