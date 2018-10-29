package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.AbstractMatrixBase;
import us.hebi.matlab.io.types.MatlabType;
import us.hebi.matlab.io.types.Sink;

import java.io.IOException;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.io.mat.Mat5WriteUtil.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
class MatMatrix extends AbstractMatrixBase implements Mat5Serializable {

    MatMatrix(int[] dims, boolean isGlobal, MatlabType type, boolean logical, NumberStore real, NumberStore imaginary) {
        super(dims, isGlobal);
        this.type = checkNotNull(type);
        this.logical = logical;

        this.real = checkNotNull(real);
        if (real.getNumElements() != getNumElements())
            throw new IllegalArgumentException("Incorrect number of elements in real store");

        this.imaginary = imaginary;
        this.complex = imaginary != null;
        if (complex && imaginary.getNumElements() != getNumElements())
            throw new IllegalArgumentException("Incorrect number of elements in imaginary store");

    }

    @Override
    public MatlabType getType() {
        return type;
    }

    @Override
    public boolean isLogical() {
        return logical;
    }

    protected void setLogical(boolean value) {
        this.logical = value;
    }

    @Override
    public boolean isComplex() {
        return complex;
    }

    @Override
    public long getLong(int index) {
        return orLogical(real.getLong(index));
    }

    @Override
    public void setLong(int index, long value) {
        real.setLong(index, value);
    }

    @Override
    public double getDouble(int index) {
        return orLogical(real.getDouble(index));
    }

    @Override
    public void setDouble(int index, double value) {
        real.setDouble(index, value);
    }

    @Override
    public long getImaginaryLong(int index) {
        return orLogical(complex ? imaginary.getLong(index) : 0);
    }

    @Override
    public void setImaginaryLong(int index, long value) {
        checkState(complex, "Matrix is not complex");
        imaginary.setLong(index, value);
    }

    @Override
    public double getImaginaryDouble(int index) {
        return orLogical(complex ? imaginary.getDouble(index) : 0);
    }

    @Override
    public void setImaginaryDouble(int index, double value) {
        checkState(complex, "Matrix is not complex");
        imaginary.setDouble(index, value);
    }

    @Override
    public int getMat5Size(String name) {
        return Mat5.MATRIX_TAG_SIZE
                + computeArrayHeaderSize(name, this)
                + real.getMat5Size()
                + (complex ? imaginary.getMat5Size() : 0);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        real.writeMat5(sink);
        if (complex) imaginary.writeMat5(sink);
    }

    /**
     * Internal API
     */
    NumberStore getRealStore() {
        return real;
    }

    @Override
    public void close() throws IOException {
        // Ignore EMPTY_MATRIX elements
        if(this == Mat5.EMPTY_MATRIX)
            return;

        real.close();
        if (imaginary != null)
            imaginary.close();
    }

    private boolean logical;
    private final NumberStore real;
    private final NumberStore imaginary;
    private final boolean complex;
    private final MatlabType type;

}
