package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.*;

import java.io.IOException;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 02 Sep 2018
 */
class MatFunction extends AbstractArray implements FunctionHandle, Mat5Serializable {

    MatFunction(boolean isGlobal, Struct content) {
        super(new int[]{1, 1}, isGlobal);
        this.content = content;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Function;
    }

    @Override
    public Struct getContent() {
        return content;
    }

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + Mat5Writer.computeArrayHeaderSize(name, this)
                + Mat5Writer.computeArraySize(getContent());
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        Mat5Writer.writeArrayWithTag(content, sink);
    }

    final Struct content;


}
