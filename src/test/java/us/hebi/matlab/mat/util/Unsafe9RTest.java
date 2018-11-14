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

import org.junit.Test;

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
public class Unsafe9RTest {

    @Test(expected = IllegalArgumentException.class)
    public void releaseHeapBuffer() {
        Unsafe9R.invokeCleaner(ByteBuffer.allocate(1));
    }

    @Test
    public void releaseDirectBuffer() {
        // Note: we can't check access after free because it'd segfault
        Unsafe9R.invokeCleaner(ByteBuffer.allocateDirect(1));
    }

    @Test
    public void unmapMemoryMappedFile() throws IOException {
        // Make sure file is gone
        File tmpFile = new File("Unsafe9RTest.tmp");
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
        Unsafe9R.invokeCleaner(buffer);
        assertTrue("post-free", tmpFile.delete());
    }

}