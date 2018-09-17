package us.hebi.matlab.common.memory;

import sun.nio.ch.DirectBuffer;

/**
 * Helps to clean up native memory. In Java 8 and below this
 * requires internal APIs that got removed in Java9. For
 * Java9 this class can be replaced using the appropriate
 * <p>
 * {@see java.lang.ref.Cleaner} API.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 31 Aug 2018
 */
public class Resources {

    public static void release(Object obj) {
        if (obj instanceof DirectBuffer)
            ((DirectBuffer) obj).cleaner().clean();
    }

}
