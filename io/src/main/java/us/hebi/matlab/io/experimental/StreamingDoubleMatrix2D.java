package us.hebi.matlab.io.experimental;

import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.mat.Mat5Serializable;
import us.hebi.matlab.io.mat.Mat5Type;
import us.hebi.matlab.io.mat.Mat5Writer;
import us.hebi.matlab.io.types.AbstractArray;
import us.hebi.matlab.io.types.MatlabType;
import us.hebi.matlab.io.types.Sink;
import us.hebi.matlab.io.types.Sinks;
import us.hebi.matlab.common.util.Silencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.io.mat.Mat5.*;
import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * Matrix that streams into a temporary storage file. The column
 * size stays fixed (e.g. group size) while the rows (e.g. time series)
 * continuously expand.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 08 May 2018
 */
public final class StreamingDoubleMatrix2D extends AbstractArray implements Mat5Serializable {

    public static StreamingDoubleMatrix2D createRowMajor(File folder, String matrixName, int numCols) throws IOException {
        return new StreamingDoubleMatrix2D(folder, matrixName, numCols);
    }

    protected StreamingDoubleMatrix2D(File folder, String name, int numCols) throws IOException {
        super(dims(0, numCols), false);
        this.name = name;
        checkNotNull(folder);
        checkState(folder.isDirectory(), "Invalid target directory: " + folder);

        // Create a temporary file for each column. The MAT file needs to be stored in column-major
        // order, so we would otherwise have to iterate through the entire file N times.
        tmpFiles = new File[numCols];
        columnSinks = new Sink[numCols];
        for (int col = 0; col < numCols; col++) {

            // Create new temporary file
            File file = new File(folder.getPath() + "/" + name + col + ".tmp");
            if (file.exists() && !file.delete()) {
                Silencer.close(null, columnSinks);
                String msg = "Failed to overwrite existing temporary storage: " + file.getAbsolutePath();
                throw new IllegalStateException(msg);
            }

            // Write buffer
            tmpFiles[col] = file;
            columnSinks[col] = Sinks.newStreamingFile(tmpFiles[col]);
        }
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Double;
    }

    public String getName() {
        return name;
    }

    public void addValue(double value) throws IOException {
        columnSinks[col++].writeDouble(value);
        if (col == getNumCols()) {
            col = 0;
            dims[0]++;
        }
    }

    @Override
    public int getMat5Size(String name) {
        int header = Mat5Writer.computeArrayHeaderSize(name, this);
        int data = Mat5Type.Double.computeSerializedSize(getNumElements());
        return Mat5.MATRIX_TAG_SIZE + header + data;
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        int numElements = getNumElements();
        Mat5Writer.writeMatrixTag(name, this, sink);
        Mat5Writer.writeArrayHeader(name, this, sink);
        Mat5Type.Double.writeTag(numElements, sink);
        writeData(sink);
        Mat5Type.Double.writePadding(numElements, sink);
    }

    private void writeData(Sink sink) throws IOException {
        if (getNumElements() == 0)
            return;

        if (sink.getByteOrder() != ByteOrder.nativeOrder())
            throw new IOException("Expected sink to be in native order");

        for (int col = 0; col < getNumCols(); col++) {

            // Make sure all data is on disk
            Silencer.close(columnSinks[col]);

            // Map each file and push data to sink
            FileInputStream input = new FileInputStream(tmpFiles[col]);
            try {
                int numBytes = getNumRows() * SIZEOF_DOUBLE;
                sink.writeInputStream(input, numBytes);
            } finally {
                Silencer.close(input);
            }

        }

    }

    @Override
    public void close() throws IOException {
        for (int col = 0; col < getNumCols(); col++) {
            if (!tmpFiles[col].delete()) {
                System.err.println("Unable to delete temporary file: " + tmpFiles[col]);
            }
        }
    }

    int col = 0;
    final File[] tmpFiles;
    final Sink[] columnSinks;
    private final String name;

}
