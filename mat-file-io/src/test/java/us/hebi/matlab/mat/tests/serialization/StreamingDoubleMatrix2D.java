/*-
 * #%L
 * Mat-File IO
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.mat.tests.serialization;

import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5Serializable;
import us.hebi.matlab.mat.format.Mat5Type;
import us.hebi.matlab.mat.types.AbstractArray;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Sinks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.matlab.common.memory.Bytes.*;
import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.mat.format.Mat5.*;
import static us.hebi.matlab.mat.format.Mat5WriteUtil.*;

/**
 * 2D Matrix that has a fixed number of columns, and an expanding number
 * of rows. We've often encountered this as an issue when working with
 * synchronized time series from multiple sources.
 * <p>
 * The MAT file format is not well suited for this because the size
 * needs to be known beforehand, and because the data is in column-major
 * format.
 *
 * This class is an example of how such a use case could be implemented
 * using custom serialization. There is one expanding file for each column
 * that contains all rows. Once the data gets written, all temporary storage
 * files get combined and written into the target sink.
 *
 * This example that is not considered part of the stable API.
 *
 * @author Florian Enner
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
                for (Sink sink : columnSinks) {
                    sink.close();
                }
                String msg = "Failed to overwrite existing temporary storage: " + file.getAbsolutePath();
                throw new IOException(msg);
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
        int header = computeArrayHeaderSize(name, this);
        int data = Mat5Type.Double.computeSerializedSize(getNumElements());
        return Mat5.MATRIX_TAG_SIZE + header + data;
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        int numElements = getNumElements();
        writeMatrixTag(name, this, sink);
        writeArrayHeader(name, this, sink);
        Mat5Type.Double.writeTag(numElements, sink);
        writeData(sink);
        Mat5Type.Double.writePadding(numElements, sink);
    }

    private void writeData(Sink sink) throws IOException {
        if (getNumElements() == 0)
            return;

        if (sink.order() != ByteOrder.nativeOrder())
            throw new IOException("Expected sink to be in native order");

        for (int col = 0; col < getNumCols(); col++) {

            // Make sure all data is on disk
            columnSinks[col].close();

            // Map each file and push data to sink
            FileInputStream input = new FileInputStream(tmpFiles[col]);
            try {
                int numBytes = getNumRows() * SIZEOF_DOUBLE;
                sink.writeInputStream(input, numBytes);
            } finally {
                input.close();
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
