package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.AbstractCell;
import us.hebi.matlab.io.types.Array;
import us.hebi.matlab.io.types.Sink;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
class MatCell extends AbstractCell implements Mat5Serializable {

    MatCell(int[] dims, boolean global) {
        super(dims, global, new Array[getNumElements(dims)]);
        Arrays.fill(contents, Mat5.EMPTY_MATRIX);
    }

    MatCell(int[] dims, boolean global, Array[] contents) {
        super(dims, global, contents);
    }

    @Override
    public int getMat5Size(String name) {
        int size = Mat5.MATRIX_TAG_SIZE;
        size += Mat5Writer.computeArrayHeaderSize(name, this);
        for (int i = 0; i < getNumElements(); i++) {
            size += Mat5Writer.computeArraySize(get(i));
        }
        return size;
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        for (int i = 0; i < getNumElements(); i++) {
            Mat5Writer.writeArrayWithTag(get(i), sink);
        }
    }

}
