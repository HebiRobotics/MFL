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
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 20 Oct 2018
 */
public class ResourcesTest {

    @Test
    public void releaseHeapBuffer() {
        // Should be NO-OP
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(0, 1);
        Resources.release(buffer);
        buffer.putInt(0, 2);
    }

    @Test
    public void releaseDirectBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(0, 1);
        Resources.release(buffer);
        // Accessing again throws segfault that can't be caught
    }

    @Test
    public void unmapMemoryMappedFile() throws IOException {
        // Make sure file is gone
        File tmpFile = new File("ResourcesTest.tmp");
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
        Resources.release(buffer);
        assertTrue("post-release", tmpFile.delete());
    }

}