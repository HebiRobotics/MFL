package us.hebi.matlab.io.experimental;

import us.hebi.matlab.common.memory.Resources;
import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.mat.Mat5File;
import us.hebi.matlab.io.mat.Mat5Writer;
import us.hebi.matlab.io.types.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

/**
 * Concurrently compresses arrays into temporary buffers,
 * and combines the output into a single file.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 08 May 2018
 */
public class ConcurrentMat5Writer {

    public ConcurrentMat5Writer(Sink sink) {
        this(sink, Deflater.BEST_SPEED);
    }

    public ConcurrentMat5Writer(Sink sink, int deflateLevel) {
        this.sink = sink;
        this.deflateLevel = deflateLevel;
    }

    public void writeFile(MatFile matFile) throws IOException {
        if (!(matFile instanceof Mat5File))
            throw new IllegalArgumentException("MatFile does not support the MAT5 format");
        writeFile((Mat5File) matFile);
    }

    public void writeFile(Mat5File matFile) throws IOException {
        matFile.writeFileHeader(sink);
        for (NamedArray entry : matFile) {
            writeRootArray(entry.getName(), entry.getValue());
        }
        try {
            finish();
        } finally {
            releaseBuffers();
        }
    }

    private void writeRootArray(final String name, final Array array) {
        tasks.add(executor.submit(new Callable<ByteBuffer>() {
            @Override
            public ByteBuffer call() throws Exception {

                // Create temporary sink
                int maxExpectedSize = Mat5Writer.computeArraySize(name, array) + 256;
                ByteBuffer buffer = allocateBuffer(maxExpectedSize);
                buffer.order(sink.getByteOrder());

                // Write compressed data to buffer
                Mat5Writer writer = Mat5.newWriter(Sinks.wrap(buffer))
                        .setDeflateLevel(deflateLevel)
                        .writeRootArray(name, array);
                writer.close();

                // Flip
                buffer.flip();
                return buffer;

            }
        }));
    }

    private void finish() throws IOException {
        for (Future<ByteBuffer> task : tasks) {

            ByteBuffer buffer = null;
            try {

                // Wait for task to finish
                buffer = task.get();

                // Copy data to output
                sink.writeByteBuffer(buffer);

            } catch (ExecutionException exe) {
                exe.printStackTrace();
                throw new IOException(exe);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                throw new IOException(ie);

            }

        }
        tasks.clear();
    }

    protected void releaseBuffers() throws IOException {
        for (ByteBuffer buffer : buffers) {
            Resources.release(buffer);
        }
        buffers.clear();
    }

    protected ByteBuffer allocateBuffer(int numBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
        buffers.add(buffer);
        return buffer;
    }

    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    private final List<Future<ByteBuffer>> tasks = new ArrayList<Future<ByteBuffer>>(64);
    private final Sink sink;
    private final int deflateLevel;

    private static final ExecutorService executor = Executors.newFixedThreadPool(16, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("mat-writer-pool-" + threadCount.getAndIncrement());
            return thread;
        }
    });
    private static AtomicInteger threadCount = new AtomicInteger(0);

}
