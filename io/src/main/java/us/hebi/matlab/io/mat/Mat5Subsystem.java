package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.AbstractArray;
import us.hebi.matlab.io.types.MatlabType;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.io.types.Sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static us.hebi.matlab.io.mat.Mat5Type.*;

/**
 * The subsystem contains various types of class information. It gets stored
 * as an int8 array at the end of a file and internally contains a separate
 * MatFile with a slightly different format.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 04 Sep 2018
 */
public final class Mat5Subsystem extends AbstractArray implements Mat5Serializable {

    Mat5Subsystem(int[] dims, boolean isGlobal, ByteBuffer buffer) {
        super(dims, isGlobal);
        this.buffer = buffer;
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
                + Mat5Writer.computeArrayHeaderSize(name, this)
                + UInt8.computeSerializedSize(getNumElements());
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        UInt8.writeByteBufferWithTag(buffer.slice(), sink);
    }

    void processReferences(McosRegistry mcosRegistry) throws IOException {
        if (mcosRegistry.getReferences().isEmpty())
            return;

        // A subsystem can have another subsystem at the end. In the tested cases
        // the nested subsystem never contained any useful data (e.g. empty opaque
        // array), so stop processing at this level.
        boolean processNestedSubsystem = false;

        // Read the mat file that is contained within the byte buffer
        Mat5File subFile = Mat5.newReader(Sources.wrap(buffer.slice()))
                .setMcosRegistry(mcosRegistry)
                .setReducedHeader(true)
                .readFile(processNestedSubsystem);

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

    final ByteBuffer buffer;

}
