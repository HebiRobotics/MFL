package us.hebi.matlab.io.types;

/**
 * Represents MATLAB's 'char' type
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Char extends Array {

    // Entire buffer in column major order
    CharSequence asCharSequence();

    // Returns first row as string if numRows is 1
    String getString();

    // Row as column string for 2D arrays
    String getRow(int row);

    char getChar(int index);

    char getChar(int row, int col);

    char getChar(int[] indices);

    void setChar(int index, char value);

    void setChar(int row, int col, char value);

    void setChar(int[] indices, char value);

}
