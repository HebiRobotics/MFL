package us.hebi.matlab.io.types;

/**
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractSparse extends AbstractMatrixBase implements Sparse {

    protected AbstractSparse(int[] dims, boolean isGlobal) {
        super(dims, isGlobal);
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Sparse;
    }

    // --- Remove integer accessors as Sparse are always double matrices

    @Override
    public long getLong(int index) {
        return (long) getDouble(index);
    }

    @Override
    public void setLong(int index, long value) {
        setDouble(index, value);
    }

    @Override
    public long getImaginaryLong(int index) {
        return (long) getImaginaryDouble(index);
    }

    @Override
    public void setImaginaryLong(int index, long value) {
        setImaginaryDouble(index, value);
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(double value) {
        this.defaultValue = value;
    }

    protected double defaultValue = 0;

}
