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

import us.hebi.matlab.mat.types.*;
import us.hebi.matlab.mat.util.Tasks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.Deflater;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;
import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 May 2018
 */
public final class Mat5Writer {

    /**
     * Sets the level for the deflate algorithm. Deflater.NO_COMPRESSION
     * disables compression entirely. The default is Deflater.BEST_SPEED.
     *
     * @param deflateLevel Deflate algorithm levels [0-9]
     * @return this
     */
    public Mat5Writer setDeflateLevel(int deflateLevel) {
        this.deflateLevel = deflateLevel;
        return this;
    }

    /**
     * Compressing elements tends to be by far the most expensive part of writing MAT5 files.
     * This method enables the deflation to happen concurrently, i.e., by multiple threads. Note
     * that this requires the compressed data to first be written into a temporary buffer, so
     * the memory usage will go up.
     * <p>
     * Non-compressed data will continue to be written using the calling thread. Arrays will
     * always be written in the input order, even if the file uses mixed compression.
     *
     * @param executorService executorService
     * @return this
     */
    public Mat5Writer enableConcurrentCompression(ExecutorService executorService) {
        return enableConcurrentCompression(executorService, Mat5.getDefaultBufferAllocator());
    }

    /**
     * In addition to enabling concurrent compression, this method also specifies how the
     * temporary buffers should be allocated. Allocated buffers get released once they are
     * no longer needed. This is useful when working with buffer pools or memory mapped buffers.
     * <p>
     * Note that for this particular case it is not necessary that the BufferAllocator zeroes
     * the allocated ByteBuffers because the data is guaranteed to be overwritten.
     *
     * @param bufferAllocator bufferAllocator
     * @return this
     */
    public Mat5Writer enableConcurrentCompression(ExecutorService executorService, BufferAllocator bufferAllocator) {
        this.executorService = checkNotNull(executorService, "empty executor service");
        this.bufferAllocator = checkNotNull(bufferAllocator, "empty buffer allocator");
        this.deflater = null;
        return this;
    }

    public Mat5Writer writeMat(MatFile matFile) throws IOException {
        if (matFile instanceof Mat5File) {
            return writeMat((Mat5File) matFile);
        }
        throw new IllegalArgumentException("MatFile does not support the MAT5 format");
    }

    private Mat5Writer writeMat(Mat5File matFile) throws IOException {
        if (!matFile.hasReducedHeader())
            headerStart = sink.position();
        matFile.writeFileHeader(sink);
        for (MatFile.Entry entry : matFile.getEntries()) {
            writeEntry(entry);
        }
        if(matFile.getSubsystem() != null){
            writeEntry(matFile.getSubsystem());
        }
        flush();
        return this;
    }

    public Mat5Writer writeEntry(MatFile.Entry entry) throws IOException {
        return writeArray(entry.getName(), entry.isGlobal(), entry.getValue());
    }

    public Mat5Writer writeArray(final String name, final Array value) throws IOException {
        return writeArray(name, false, value);
    }

    public Mat5Writer writeArray(final String name, final boolean isGlobal, final Array array) throws IOException {
        if ((name == null || name.isEmpty())
                && !(array instanceof McosReference)
                && !(array instanceof Mat5Subsystem))
            throw new IllegalArgumentException("Root Array can't have an empty name");
        final boolean isSubsystem = array instanceof Mat5Subsystem;

        if (deflateLevel == Deflater.NO_COMPRESSION) {
            // Uncompressed writes can always be done without using a buffer

            if (flushActions.isEmpty()) {

                // No queue, so we can write immediately
                if (isSubsystem) nextEntryIsSubsystem();
                Mat5WriteUtil.writeArray(name, isGlobal, array, sink);

            } else {

                // Queue action to preserve input order
                FlushAction action = new FlushAction() {
                    public void run() throws IOException {
                        if (isSubsystem) nextEntryIsSubsystem();
                        Mat5WriteUtil.writeArray(name, isGlobal, array, sink);
                    }
                };
                flushActions.add(Tasks.wrapAsFuture(action));

            }

        } else if (executorService == null) {

            // Do single threaded compressions immediately. Note that actions are only
            // added on concurrent writes, which means that executorService can't be null.
            checkState(flushActions.isEmpty(), "Expected flush actions to be empty when writing single threaded");
            if (isSubsystem) nextEntryIsSubsystem();

            // Reuse deflater
            if (deflater == null) {
                deflater = new Deflater(deflateLevel);
            } else {
                deflater.setLevel(deflateLevel);
                deflater.reset();
            }

            Mat5WriteUtil.writeArrayDeflated(name, isGlobal, array, sink, deflater);
            return this;

        } else {

            // Write compressed entries into temporary buffers, and combine them in flush action
            final Deflater deflater = new Deflater(deflateLevel);
            final BufferAllocator bufferAllocator = this.bufferAllocator;
            flushActions.add(executorService.submit(new Callable<FlushAction>() {
                @Override
                public FlushAction call() throws Exception {

                    // Create temporary buffer
                    int maxExpectedSize = computeArraySize(name, array) + 256;
                    final ByteBuffer buffer = bufferAllocator.allocate(maxExpectedSize);
                    Sink tmpSink = Sinks.wrap(buffer).order(sink.order());

                    // Compress async into temporary buffer
                    Mat5WriteUtil.writeArrayDeflated(name, isGlobal, array, tmpSink, deflater);
                    tmpSink.close();
                    buffer.flip();

                    // Combine in flushing thread
                    return new FlushAction() {
                        public void run() throws IOException {
                            try {
                                if (isSubsystem) nextEntryIsSubsystem();
                                sink.writeByteBuffer(buffer);
                            } finally {
                                bufferAllocator.release(buffer);
                            }
                        }
                    };
                }
            }));

        }

        return this;
    }

    private void nextEntryIsSubsystem() throws IOException {
        this.subsysLocation = sink.position();
    }

    /**
     * Makes sure that all written arrays were written to the sink, and that
     * the (optional) subsystem offset has been set. May be called more than
     * once.
     *
     * @return this
     * @throws IOException if writing to the Sink fails
     */
    public Mat5Writer flush() throws IOException {
        // Write all entries
        for (Future<FlushAction> action : flushActions) {
            try {
                action.get().run();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        // Lastly, update subsystem offset in the (non-reduced) header
        if (headerStart >= 0 && subsysLocation > 0) {
            Mat5File.updateSubsysOffset(headerStart, subsysLocation, sink);
            subsysLocation = 0;
        }
        return this;
    }

    private interface FlushAction {
        void run() throws IOException;
    }

    Mat5Writer(Sink sink) {
        this.sink = checkNotNull(sink, "Sink can't be empty");
    }

    protected final Sink sink;
    protected int deflateLevel = Deflater.BEST_SPEED;
    private long headerStart = -1;
    private long subsysLocation = 0; // matlab uses zeros for missing subsystems
    private ExecutorService executorService = null;
    private BufferAllocator bufferAllocator = Mat5.getDefaultBufferAllocator();
    private final List<Future<FlushAction>> flushActions = new ArrayList<Future<FlushAction>>(16);
    private Deflater deflater = null;

}
