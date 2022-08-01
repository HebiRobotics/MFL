package us.hebi.matlab.mat.format7;

import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Node;
import io.jhdf.dataset.DatasetBase;
import us.hebi.matlab.mat.types.MatFile;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

/**
 * Reads MAT 7.3 files based on HDF
 *
 * @author Florian Enner
 * @since 20 Mai 2022
 */
public final class Mat7Reader implements Closeable {

    public Mat7Reader(HdfFile hdfFile) {
        this.hdfFile = hdfFile;
    }

    public final Mat7File readMat() throws IOException {
        Mat7File matFile = new Mat7File();
        for (Node node : hdfFile) {
            MatFile.Entry entry = convertToEntry(node);
            if (entry != null) {
                matFile.addEntry(entry);
            }
        }
        return matFile;
    }

    @Override
    public void close() throws IOException {
        hdfFile.close();
    }

    private MatFile.Entry convertToEntry(Node node) {
        String matlabClass = Optional.ofNullable(node.getAttribute("MATLAB_class"))
                .map(Attribute::getData)
                .map(String::valueOf)
                .orElse(null);

        if (matlabClass == null) {
            return null;
        }
        System.out.println("MATLAB_class = " + matlabClass);
        System.out.println("node.isGroup() = " + node.isGroup());
        System.out.println("node.isLink() = " + node.isLink());

        // Sparse are separate
        if (node.getAttribute("MATLAB_sparse") != null) {
            // TODO: parse sparse
            return null;
        }

        // Other types
        switch (matlabClass) {
            case "double":
            case "single":
            case "uint8":
            case "uint32":
                DatasetBase ds = (DatasetBase) node;
                System.out.println("ds.getDimensions() = " + Arrays.toString(ds.getDimensions()));
                System.out.println("ds.getDataBuffer() = " + ds.getDataBuffer());

                ByteBuffer buffer = ds.getDataBuffer(); // column-major!
                while (buffer.remaining() > 0) {
                    System.out.print(buffer.getDouble() + ", ");
                }
                System.out.println();

                Object data = ds.getData();
                System.out.println(data);
            default:
                return null;
        }
    }

    private final HdfFile hdfFile;


}
