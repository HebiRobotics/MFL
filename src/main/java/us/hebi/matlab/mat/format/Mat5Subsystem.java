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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * The subsystem contains various types of class information. It gets stored
 * as an int8 array at the end of a file and internally contains a separate
 * MatFile with a slightly different format.
 *
 * @author Florian Enner
 * @since 04 Sep 2018
 */
public final class Mat5Subsystem extends AbstractArray implements Mat5Serializable {

    Mat5Subsystem(int[] dims, ByteBuffer buffer, BufferAllocator bufferAllocator) {
        super(dims);
        this.buffer = buffer;
        this.bufferAllocator = bufferAllocator;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.UInt8;
    }

    public ByteBuffer getBuffer() {
        return buffer.slice();
    }

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + UInt8.computeSerializedSize(getNumElements());
    }

    @Override
    public void writeMat5(String name, boolean isGlobal, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, isGlobal, this, sink);
        UInt8.writeByteBufferWithTag(buffer.slice(), sink);
    }

    void processReferences(McosRegistry mcosRegistry) throws IOException {
        if (mcosRegistry.getReferences().isEmpty())
            return;

        // Read the mat file that is contained within the byte buffer
        subFile = Mat5.newReader(Sources.wrap(buffer.slice()))
                .setMcosRegistry(mcosRegistry)
                .setBufferAllocator(bufferAllocator)
                .disableSubsystemProcessing() // the Subsystem's subsystem does not contain useful data
                .setReducedHeader(true)
                .readMat();

        // The first entry in the top level subsystem (end of root file) contains the
        // 'FileWrapper__' object which contains the data backing the various reference
        // classes, e.g., handles. Note that more than one references can reference the
        // same data, and that the referenced objects may themselves be references.
        McosFileWrapper fileWrapper = subFile.getStruct(0).get("MCOS");
        List<McosObject> objects = fileWrapper.parseObjects(mcosRegistry);

        // Update references of all handle classes
        objects.add(0, null); // bump count to match off-by-one index
        for (McosReference reference : mcosRegistry.getReferences()) {
            reference.setReferences(objects);
        }

    }

    @Override
    public void close() throws IOException {
        if (subFile != null)
            subFile.close();
        bufferAllocator.release(buffer);
        buffer = null;
        bufferAllocator = null;
        subFile = null;
    }

    private ByteBuffer buffer;
    private BufferAllocator bufferAllocator;
    private Mat5File subFile;

    @Override
    protected int subHashCode() {
        return buffer.hashCode();
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        Mat5Subsystem other = (Mat5Subsystem) otherGuaranteedSameClass;
        return other.buffer.equals(buffer);
    }
}
