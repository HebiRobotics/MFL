package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.AbstractCell;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;
import java.util.Arrays;

import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
class MatCell extends AbstractCell implements Mat5Serializable {

    MatCell(int[] dims, boolean global) {
        super(dims, global, new Array[getNumElements(dims)]);
        Arrays.fill(contents, getEmptyValue());
    }

    MatCell(int[] dims, boolean global, Array[] contents) {
        super(dims, global, contents);
    }

    @Override
    protected Array getEmptyValue() {
        return Mat5.EMPTY_MATRIX;
    }

    @Override
    public int getMat5Size(String name) {
        int size = Mat5.MATRIX_TAG_SIZE;
        size += computeArrayHeaderSize(name, this);
        for (int i = 0; i < getNumElements(); i++) {
            size += computeArraySize(get(i));
        }
        return size;
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        for (int i = 0; i < getNumElements(); i++) {
            writeArrayWithTag(get(i), sink);
        }
    }

}
