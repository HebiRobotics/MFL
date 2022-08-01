package us.hebi.matlab.mat.format7;

import us.hebi.matlab.mat.types.AbstractMatFile;
import us.hebi.matlab.mat.types.MatFile;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * @author Florian Enner
 * @since 20 Mai 2022
 */
public class Mat7File extends AbstractMatFile {

    @Override
    public long getUncompressedSerializedSize() {
        throw new UnsupportedOperationException("This library does not support writing the MAT-file format v7.3");
    }

    @Override
    public MatFile writeTo(Sink sink) throws IOException {
        throw new UnsupportedOperationException("This library does not support writing the MAT-file format v7.3");
    }

    @Override
    protected int subHashCode() {
        return 0;
    }

    @Override
    protected boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass) {
        return false;
    }

    @Override
    public String toString() {
        return "Mat7File\n" + super.toString();
    }

}
