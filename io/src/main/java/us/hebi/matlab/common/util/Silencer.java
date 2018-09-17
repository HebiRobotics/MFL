package us.hebi.matlab.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides helper functions to remove rarely used try catch blocks
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 26 Sep 2014
 */
public class Silencer {

    /**
     * Sleeps the given amount of time and converts the checked
     * InterruptException to an unchecked exception.
     *
     * @param amount
     * @param unit
     */
    public static void sleep(long amount, TimeUnit unit) {
        try {
            unit.sleep(amount);
        } catch (InterruptedException e) {
            // throw runtime exception just in case it actually
            // gets interrupted.
            throw new RuntimeException(e);
        }
    }

    public static void sleepSeconds(long amount) {
        sleep(amount, TimeUnit.SECONDS);
    }

    public static void sleepMillis(long amount) {
        sleep(amount, TimeUnit.MILLISECONDS);
    }

    /**
     * Closes the specified {@code Closeable} resources (e.g. socket, stream)
     * and ignores any IOException thrown by the close operation.
     *
     * @param resource  the resource to close, may be {@code null}
     * @param resources further resources to close, may be empty or null {@code null}
     */
    public static void close(final Closeable resource, final Closeable... resources) {
        closeResourceSilently(resource);
        for (Closeable closeable : resources) {
            closeResourceSilently(closeable);
        }
    }

    /**
     * Closes the specified {@code Closeable} resources (e.g. socket, stream)
     * and ignores any IOException thrown by the close operation.
     *
     * @param resources a list of resources to be close, may not be {@code null}
     */
    public static void close(List<Closeable> resources) {
        if (resources == null) throw new NullPointerException();
        for (Closeable closeable : resources) {
            closeResourceSilently(closeable);
        }
    }

    private static void closeResourceSilently(final Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final IOException ignored) {
                // ignored
            }
        }
    }
}
