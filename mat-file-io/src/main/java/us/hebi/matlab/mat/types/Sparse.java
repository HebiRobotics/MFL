package us.hebi.matlab.mat.types;

/**
 * Represents MATLAB's sparse matrix types
 * Behavior:
 * - Always two dimensional
 * - May not be logical
 * - May be complex
 * - Data is internally always stored as double
 * <p>
 * Note that index based access works on the non-zero values,
 * so there are only nzMax values.
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Sparse extends Matrix {

    /**
     * @return number of non-zero elements
     */
    int getNzMax();

    double getDefaultValue();

    void setDefaultValue(double value);

    /**
     * Performs the supplied action for each non-zero value
     */
    void forEach(SparseConsumer action);

    interface SparseConsumer {
        void accept(int row, int col, double real, double imaginary);
    }

}
