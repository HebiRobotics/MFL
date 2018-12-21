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

package us.hebi.matlab.mat.format;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * @author Florian Enner
 * @since 08 Sep 2018
 */
class Charsets {

    private Charsets() {
    }

    // Charsets that are included in Java platform specification
    // See https://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

    // Optional charsets that may or may not be supported
    public static final Charset UTF_32BE = forNameOrNull("UTF-32BE");
    public static final Charset UTF_32LE = forNameOrNull("UTF-32LE");

    private static Charset forNameOrNull(String name) {
        try {
            return Charset.forName(name);
        } catch (UnsupportedCharsetException uce) {
            return null;
        }
    }
}
