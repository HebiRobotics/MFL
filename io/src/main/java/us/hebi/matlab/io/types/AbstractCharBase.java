package us.hebi.matlab.io.types;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 07 Sep 2018
 */
public abstract class AbstractCharBase extends AbstractArray implements Char {

    protected AbstractCharBase(int[] dims, boolean isGlobal) {
        super(dims, isGlobal);
    }

    // ---- To be implemented

    @Override
    public abstract CharSequence asCharSequence();

    @Override
    public abstract char getChar(int index);

    @Override
    public abstract void setChar(int index, char value);

    // ---- Convenience accessors

    @Override
    public MatlabType getType() {
        return MatlabType.Character;
    }

    @Override
    public String getString() {
        if (getNumRows() != 1)
            throw new IllegalStateException("Char is not a single row char string");
        return getRow(0);
    }

    @Override
    public String getRow(int row) {
        checkNumDimensions(2);
        int numCols = getNumCols();

        // thread local might be too large and use is likely single threaded
        synchronized (builder) {
            // Make sure there is enough space
            builder.ensureCapacity(numCols);
            builder.setLength(0);

            // Read until end of string character
            for (int col = 0; col < numCols; col++) {
                char c = getChar(row, col);
                if (c == '\0')
                    break;
                builder.append(c);
            }
            return builder.toString();
        }
    }

    @Override
    public char getChar(int row, int col) {
        return getChar(getColumnMajorIndex(row, col));
    }

    @Override
    public char getChar(int[] indices) {
        return getChar(getColumnMajorIndex(indices));
    }

    @Override
    public void setChar(int row, int col, char value) {
        setChar(getColumnMajorIndex(row, col), value);
    }

    @Override
    public void setChar(int[] indices, char value) {
        setChar(getColumnMajorIndex(indices), value);
    }

    protected final StringBuilder builder = new StringBuilder();

}
