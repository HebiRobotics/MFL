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

package us.hebi.matlab.mat.format.mat5test;

import us.hebi.matlab.common.memory.Unsafe9R;
import us.hebi.matlab.mat.format.BufferAllocator;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.util.Casts.*;
import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * @author Florian Enner
 * @since 08 Sep 2018
 */
public class MatTestUtil {

    public static Mat5File readMat(String name) throws IOException {
        return readMat(name, testRoundTrip);
    }

    public static Mat5File readMat(String name, boolean testRoundTrip) throws IOException {
        return readMat(name, testRoundTrip, false);
    }

    public static Mat5File readMat(String name, boolean testRoundTrip, boolean reduced) throws IOException {
        // Read from original file using single-threaded reader
        Mat5File results = readMat(MatTestUtil.class, name, reduced);
        if (debugPrint) {
            System.out.println("source = " + results);
            for (NamedArray result : results) {
                System.out.println(result);
            }
        }

        if (!testRoundTrip)
            return results;

        // Write contents to buffer using single threaded writer
        int errorPadding = 128;
        long maxSize = results.getUncompressedSerializedSize() + errorPadding;
        ByteBuffer buffer = ByteBuffer.allocate(sint32(maxSize));
        try (Sink sink = Sinks.wrap(buffer).order(testOrder)) {
            Mat5.newWriter(sink)
                    .setDeflateLevel(deflateLevel)
                    .writeMat(results);
        }

        // Write contents to a 2nd buffer using concurrent writer
        ByteBuffer buffer2 = ByteBuffer.allocate(sint32(maxSize));
        try (Sink sink = Sinks.wrap(buffer2).order(testOrder)) {
            Mat5.newWriter(sink)
                    .setDeflateLevel(deflateLevel)
                    .enableConcurrentCompression(executorService)
                    .writeMat(results);
        }

        // Make sure content of buffer 1 & 2 are the same
        buffer.flip();
        buffer2.flip();
        assertEquals("Mismatch between single threaded and concurrent writer", buffer, buffer2);

        // Make sure close() accounts for all created buffers
        buffer.rewind();
        try (Source source = Sources.wrap(buffer)) {
            BufferLeakChecker leakChecker = new BufferLeakChecker();
            Mat5.newReader(source)
                    .setBufferAllocator(leakChecker)
                    .setReducedHeader(reduced)
                    .readMat()
                    .close();
            leakChecker.verifyAllReleased();
        }

        // Read data using concurrent reader
        buffer.rewind();
        try (Source source = Sources.wrap(buffer)) {

            Mat5File file = Mat5.newReader(source)
                    .enableConcurrentDecompression(executorService)
                    .setReducedHeader(reduced)
                    .readMat();

            if (debugPrint) {
                System.out.println("roundtrip = " + file);
                for (NamedArray result : file) {
                    System.out.println(result);
                }
            }
            return file;
        }

    }

    private static class BufferLeakChecker implements BufferAllocator {

        int numAllocated = 0;
        int numReleased = 0;

        @Override
        public synchronized ByteBuffer allocate(int numBytes) {
            // Allocate a buffer with a larger capacity to make sure that
            // everything adheres to the contract
            numAllocated++;
            ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes + 8);
            buffer.limit(numBytes);
            return buffer;
        }

        @Override
        public synchronized void release(ByteBuffer buffer) {
            numReleased++;
            Unsafe9R.invokeCleaner(buffer);
        }

        synchronized void verifyAllReleased() {
            if (numReleased != numAllocated) {
                throw new IllegalStateException((numAllocated - numReleased) + " buffers were not accounted for");
            }
        }

    }

    private static Mat5File readMat(Class location, String name, boolean reduced) throws IOException {
        try (Source source = getSource(location, name)) {
            return Mat5.newReader(source).setReducedHeader(reduced).readMat();
        }
    }

    private static Source getSource(Class clazz, String name) throws IOException {
        // Read mat into buffer
        InputStream inputStream = clazz.getResourceAsStream(name);
        checkNotNull(inputStream, "File %s could not be found", name);
        ByteBuffer buffer = ByteBuffer.allocate(inputStream.available());
        int bytes = inputStream.read(buffer.array());
        if (bytes != buffer.array().length) {
            throw new AssertionError("Could not read full contents of " + name);
        }
        inputStream.close();

        // Wrap buffer as source
        return Sources.wrap(buffer);
    }

    private static boolean testRoundTrip = true; // enabled does read/write/read before checking the data
    static boolean debugPrint = false;
    private static ByteOrder testOrder = ByteOrder.nativeOrder();
    private static int deflateLevel = Deflater.BEST_SPEED;
    static double DELTA = 0; // should read exactly the same bits
    static float FLOAT_DELTA = 1E-6f;

    static ExecutorService executorService = Executors.newCachedThreadPool();

}
