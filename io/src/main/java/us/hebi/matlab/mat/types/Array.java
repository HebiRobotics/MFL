package us.hebi.matlab.mat.types;

import java.io.Closeable;

/**
 * Represents the 'miMATRIX' type that is used to represent
 * all kinds of named variables inside a MAT file. This includes
 * N-dimensional numerical arrays as well as aggregate types such
 * as structs or cell arrays.
 * <p>
 * Note that all MATLAB variables are implemented as N-dimensional arrays,
 * and that singular values or structs are arrays with 1 row and 1 column.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 02 May 2018
 */
public interface Array extends Closeable {

    MatlabType getType();

    int[] getDimensions();

    int getNumDimensions();

    int getNumRows();

    int getNumCols();

    int getNumElements();

    boolean isGlobal();

    void setGlobal(boolean global);

}
