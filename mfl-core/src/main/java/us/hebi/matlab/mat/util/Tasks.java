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

package us.hebi.matlab.mat.util;


import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Florian Enner
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
