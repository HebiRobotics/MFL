package us.hebi.matlab.mat.format.experimental;

import org.ejml.data.DMatrix;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5Serializable;
import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

import static us.hebi.matlab.mat.format.Mat5Type.Double;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * Serializes an EJML double matrix into a MAT 5 file that can be read by MATLAB
 *
 * @author Florian Enner
 */
public class EjmlDMatrixWrapper extends AbstractArray implements Mat5Serializable {

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + Double.computeSerializedSize(matrix.getNumElements());
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);

        // Data in column major format
        Double.writeTag(matrix.getNumElements(), sink);
        for (int col = 0; col < matrix.getNumCols(); col++) {
            for (int row = 0; row < matrix.getNumRows(); row++) {
                sink.writeDouble(matrix.unsafe_get(row, col));
            }
        }
        Double.writePadding(matrix.getNumElements(), sink);

    }

    @Override
    public MatlabType getType() {
        return MatlabType.Double;
    }

   public EjmlDMatrixWrapper(DMatrix matrix) {
        super(Mat5.dims(matrix.getNumRows(), matrix.getNumCols()), false);
        this.matrix = matrix;
    }

    @Override
    public void close() throws IOException {
    }

    final DMatrix matrix;

}
