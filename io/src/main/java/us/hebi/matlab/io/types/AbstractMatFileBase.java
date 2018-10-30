package us.hebi.matlab.io.types;

/**
 * Forwards convenience overloads to avoid cluttering the implementation
 *
 * @author Florian Enner
 * @since 14 Sep 2018
 */
public abstract class AbstractMatFileBase implements MatFile {

    @Override
    public Matrix getMatrix(String name) {
        return getArray(name);
    }

    @Override
    public Sparse getSparse(String name) {
        return getArray(name);
    }

    @Override
    public Char getChar(String name) {
        return getArray(name);
    }

    @Override
    public Struct getStruct(String name) {
        return getArray(name);
    }

    @Override
    public ObjectStruct getObject(String name) {
        return getArray(name);
    }

    @Override
    public Cell getCell(String name) {
        return getArray(name);
    }

    @Override
    public Matrix getMatrix(int index) {
        return getArray(index);
    }

    @Override
    public Sparse getSparse(int index) {
        return getArray(index);
    }

    @Override
    public Char getChar(int index) {
        return getArray(index);
    }

    @Override
    public Struct getStruct(int index) {
        return getArray(index);
    }

    @Override
    public ObjectStruct getObject(int index) {
        return getArray(index);
    }

    @Override
    public Cell getCell(int index) {
        return getArray(index);
    }

}
