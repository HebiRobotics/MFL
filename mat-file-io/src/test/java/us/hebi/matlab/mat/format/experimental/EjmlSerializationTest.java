package us.hebi.matlab.mat.format.experimental;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.types.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.matlab.common.util.Casts.*;

/**
 * Demonstrates serialization of custom data sets by writing
 * EJML's sparse and dense matrices.
 *
 * @author Florian Enner
 * @since 16 Sep 2018
 */
public class EjmlSerializationTest {

    @Test
    public void testDense() throws Exception {
        DMatrixRMaj dense = new DMatrixRMaj(18, 27);
        for (int i = 0; i < dense.data.length; i++) {
            dense.data[i] = rnd.nextDouble();
        }

        // Create file structure and serialize
        MatFile mat = Mat5.newMatFile().addArray("name", new EjmlDMatrixWrapper(dense));
        Matrix result = writeReadMat(mat).getMatrix("name");

        // Test output
        for (int col = 0; col < dense.getNumCols(); col++) {
            for (int row = 0; row < dense.getNumRows(); row++) {
                assertEquals(dense.get(row, col), result.getDouble(row, col), 0);
            }
        }

    }

    @Test
    public void testSparse() throws Exception {
        DMatrixSparseCSC sparse = new DMatrixSparseCSC(18, 21, 10);
        sparse.set(3, 8, rnd.nextDouble());
        sparse.set(12, 18, rnd.nextDouble());
        sparse.set(7, 9, rnd.nextDouble());
        sparse.set(3, 6, rnd.nextDouble());
        sparse.sortIndices(null);

        // Create file structure and serialize
        MatFile mat = Mat5.newMatFile().addArray("name", new EjmlSparseWrapper(sparse));
        Sparse result = writeReadMat(mat).getSparse("name");

        // Test output
        assertEquals(sparse.getNonZeroLength(), result.getNzMax());
        for (int col = 0; col < sparse.getNumCols(); col++) {
            for (int row = 0; row < sparse.getNumRows(); row++) {
                assertEquals(sparse.get(row, col), result.getDouble(row, col), 0);
            }
        }

    }

    private MatFile writeReadMat(MatFile mat) throws IOException {
        // Write to buffer and read result
        ByteBuffer buffer = ByteBuffer.allocate(sint32(mat.getUncompressedSerializedSize()));
        mat.writeTo(Sinks.wrap(buffer));
        buffer.flip();
        return Mat5.newReader(Sources.wrap(buffer)).readMat();
    }

    final Random rnd = new Random(0);

}
