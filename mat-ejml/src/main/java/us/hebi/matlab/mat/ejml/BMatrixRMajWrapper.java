package us.hebi.matlab.mat.ejml;

import org.ejml.data.BMatrixRMaj;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * Serializes an EJML boolean matrix
 *
 * @author Florian Enner
 */
class BMatrixRMajWrapper extends AbstractMatrixWrapper<BMatrixRMaj> {

    @Override
    protected int getMat5DataSize() {
        return Mat5Type.UInt8.computeSerializedSize(matrix.getNumElements());
    }

    @Override
    protected void writeMat5Data(Sink sink) throws IOException {
        Mat5Type.UInt8.writeTag(matrix.getNumElements(), sink);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                int value = matrix.unsafe_get(row, col) ? 1 : 0;
                sink.writeByte((byte) value);
            }
        }
        Mat5Type.UInt8.writePadding(matrix.getNumElements(), sink);
    }

    @Override
    public MatlabType getType() {
        return MatlabType.UInt8;
    }

    @Override
    public boolean isLogical() {
        return true;
    }

    protected BMatrixRMajWrapper(BMatrixRMaj matrix) {
        super(matrix);
    }

}
