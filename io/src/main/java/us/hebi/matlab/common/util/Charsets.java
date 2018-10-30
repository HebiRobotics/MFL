package us.hebi.matlab.common.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * @author Florian Enner
 * @since 08 Sep 2018
 */
public class Charsets {

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
