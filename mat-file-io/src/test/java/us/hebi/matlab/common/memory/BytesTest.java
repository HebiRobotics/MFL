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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 27 Aug 2018
 */
public class BytesTest {

    @Test
    public void nextPowerOfTwo() {
        assertEquals(1, Bytes.nextPowerOfTwo(0));
        assertEquals(8, Bytes.nextPowerOfTwo(5));
        assertEquals(8, Bytes.nextPowerOfTwo(8));
        assertEquals(4, Bytes.nextPowerOfTwo(4));
        assertEquals(1024, Bytes.nextPowerOfTwo(513));
        assertEquals(256, Bytes.nextPowerOfTwo(256));
    }

}
