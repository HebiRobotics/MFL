package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.*;

import java.io.IOException;

import static us.hebi.matlab.io.mat.Mat5WriteUtil.*;

/**
 * @author Florian Enner
 * @since 04 Sep 2018
 */
class MatOpaque extends AbstractArray implements Opaque, Mat5Serializable {

    MatOpaque(boolean isGlobal, String objectType, String className, Array content) {
        super(SINGLE_DIM, isGlobal);
        this.content = content;
        this.className = className;
        this.objectType = objectType;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Opaque;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getClassName() {
        return className;
    }

    public Array getContent() {
        return content;
    }

    @Override
    public int getMat5Size(String name) {
        return computeOpaqueSize(name, this);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeOpaque(name, this, sink);
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    private final String objectType;
    private final String className;
    private final Array content;
    private static final int[] SINGLE_DIM = new int[]{1, 1};

}
