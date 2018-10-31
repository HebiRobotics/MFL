package us.hebi.matlab.mat.types;

/**
 * Forwards convenience overloads to a minimum required set
 *
 * @author Florian Enner
 * @since 07 Sep 2018
 */
public abstract class AbstractCellBase extends AbstractArray implements Cell {

    protected AbstractCellBase(int[] dims, boolean isGlobal) {
        super(dims, isGlobal);
    }

    // ---- Actual Accessors

    @Override
    public abstract <T extends Array> T get(int index);

    @Override
    public abstract Cell set(int index, Array value);

    // ---- Convenience overloads

    @Override
    public MatlabType getType() {
        return MatlabType.Cell;
    }

    @Override
    public Matrix getMatrix(int index) {
        return get(index);
    }

    @Override
    public Matrix getMatrix(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Matrix getMatrix(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Sparse getSparse(int index) {
        return get(index);
    }

    @Override
    public Sparse getSparse(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Sparse getSparse(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Char getChar(int index) {
        return get(index);
    }

    @Override
    public Char getChar(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Char getChar(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Struct getStruct(int index) {
        return get(index);
    }

    @Override
    public Struct getStruct(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Struct getStruct(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Cell getCell(int index) {
        return get(index);
    }

    @Override
    public Cell getCell(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public Cell getCell(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public <T extends Array> T get(int row, int col) {
        return get(getColumnMajorIndex(row, col));
    }

    @Override
    public <T extends Array> T get(int[] indices) {
        return get(getColumnMajorIndex(indices));
    }

    @Override
    public Cell set(int row, int col, Array value) {
        set(getColumnMajorIndex(row, col), value);
        return this;
    }

    @Override
    public Cell set(int[] indices, Array value) {
        set(getColumnMajorIndex(indices), value);
        return this;
    }

}
