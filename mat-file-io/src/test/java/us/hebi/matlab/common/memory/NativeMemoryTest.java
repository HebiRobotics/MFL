package us.hebi.matlab.common.memory;

import org.junit.Test;
import us.hebi.matlab.common.util.PlatformInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 20 Oct 2018
 */
public class NativeMemoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void releaseHeapBuffer() {
        NativeMemory.freeDirectBuffer(ByteBuffer.allocate(1));
    }

    @Test
    public void releaseDirectBuffer() {
        // Note: we can't check access after free because it'd segfault
        NativeMemory.freeDirectBuffer(ByteBuffer.allocateDirect(1));
    }

    @Test
    public void unmapMemoryMappedFile() throws IOException {
        // Make sure file is gone
        File tmpFile = new File("NativeMemoryTest.tmp");
        assertTrue("pre-create", !tmpFile.exists() || tmpFile.delete());

        // Create new file
        final FileChannel channel = new RandomAccessFile(tmpFile, "rw").getChannel();
        assertTrue("post-create", tmpFile.exists());

        // Map to memory
        final ByteBuffer buffer = channel
                .map(FileChannel.MapMode.READ_WRITE, 0, 1024)
                .load();

        if (PlatformInfo.isWindows()) {
            // Windows keeps open files from being deleted
            assertFalse("pre-close", tmpFile.delete());
            channel.close();
            assertFalse("post-close", tmpFile.delete());
        } else {
            // Linux doesn't
            channel.close();
        }

        // Unmap memory
        NativeMemory.freeDirectBuffer(buffer);
        assertTrue("post-free", tmpFile.delete());
    }

}