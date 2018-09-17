package us.hebi.matlab.common.util;


import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 03 May 2018
 */
public class Tasks {

    public static <T> Future<T> wrapAsFuture(T result) {
        return new PrecomputedFuture<T>(result);
    }

    // Callable with IOException rather than Exception
    public interface IoTask<V> extends Callable<V> {
        V call() throws IOException;
    }

    private static class PrecomputedFuture<T> implements Future<T> {

        private PrecomputedFuture(T result) {
            this.result = result;
        }

        final T result;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() {
            return result;
        }

        @Override
        public T get(long timeout, TimeUnit unit) {
            return result;
        }

    }

}
