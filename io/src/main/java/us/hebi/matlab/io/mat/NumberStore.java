package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.Sink;

import java.io.Closeable;
import java.io.IOException;

/**
 * Provides a way to store numerical data with different types and
 * can be thought of as an array.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 03 May 2018
 */
interface NumberStore extends Closeable {

    int getNumElements();

    long getLong(int index);

    void setLong(int index, long value);

    double getDouble(int index);

    void setDouble(int index, double value);

    int getMat5Size();

    void writeMat5(Sink sink) throws IOException;

}
