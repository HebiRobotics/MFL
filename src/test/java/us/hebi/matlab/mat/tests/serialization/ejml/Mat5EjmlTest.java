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

package us.hebi.matlab.mat.tests.serialization.ejml;

import org.ejml.EjmlUnitTests;
import org.ejml.data.*;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.ops.ConvertFMatrixStruct;
import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.types.MatFile;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Sinks;
import us.hebi.matlab.mat.types.Sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.ejml.UtilEjml.*;
import static us.hebi.matlab.mat.tests.serialization.ejml.Mat5Ejml.*;
import static us.hebi.matlab.mat.util.Casts.*;

/**
 * Demonstrates serialization of custom data sets by writing
 * EJML's sparse and dense matrices.
 *
 * @author Florian Enner
 * @since 16 Sep 2018
 */
public class Mat5EjmlTest {

    @Test
    public void testMixedMatrices() throws Exception {

        // Save EJML Matrices
        MatFile mat = Mat5.newMatFile()
                .addArray("F", asArray(new FMatrixRMaj(rows, cols)))
                .addArray("D", asArray(new DMatrixRMaj(rows, cols)))
                .addArray("C", asArray(new CMatrixRMaj(rows, cols)))
                .addArray("Z", asArray(new ZMatrixRMaj(rows, cols)));

        MatFile result = writeReadMat(mat);

        // Load EJML Matrices
        FMatrixRMaj fmatrix = convert(result.getArray("F"), new FMatrixRMaj(0, 0));
        DMatrixRMaj dmatrix = convert(result.getArray("D"), new DMatrixRMaj(0, 0));
        CMatrixRMaj cmatrix = convert(result.getArray("C"), new CMatrixRMaj(0, 0));
        ZMatrixRMaj zmatrix = convert(result.getArray("Z"), new ZMatrixRMaj(0, 0));

    }

    @Test
    public void testFMatrix() throws Exception {
        FMatrixRMaj expected = new FMatrixRMaj(rows, cols);
        fillData(expected.data);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F32);
    }

    @Test
    public void testFMatrix3x3() throws Exception {
        FMatrix3x3 expected = new FMatrix3x3(0, 1, 2, 3, 4, 5, 6, 7, 8);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F32);
    }

    @Test
    public void testDMatrix() throws Exception {
        DMatrixRMaj expected = new DMatrixRMaj(rows, cols);
        fillData(expected.data);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F64);
    }

    @Test
    public void testDMatrix3x3() throws Exception {
        DMatrix3x3 expected = new DMatrix3x3(0, 1, 2, 3, 4, 5, 6, 7, 8);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F64);
    }

    @Test
    public void testCMatrix() throws Exception {
        CMatrixRMaj expected = new CMatrixRMaj(rows, cols);
        fillData(expected.data);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F32);
    }

    @Test
    public void testZMatrix() throws Exception {
        ZMatrixRMaj expected = new ZMatrixRMaj(rows, cols);
        fillData(expected.data);
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F64);
    }

    @Test
    public void testDMatrixSparseCSC() throws Exception {
        DMatrixSparseCSC expected = new DMatrixSparseCSC(18, 21, 10);
        expected.set(3, 8, rnd.nextDouble());
        expected.set(12, 18, rnd.nextDouble());
        expected.set(7, 9, rnd.nextDouble());
        expected.set(3, 6, rnd.nextDouble());
        expected.sortIndices(null);

        // Read CSC
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F64);

        // Read Triplet
        DMatrixSparseTriplet triplet = saveAndLoad(expected, new DMatrixSparseTriplet());
        DMatrixSparseCSC actual = ConvertDMatrixStruct.convert(triplet, expected.createLike());
        EjmlUnitTests.assertEquals(expected, actual, TEST_F64);

        // Read Dense
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, new DMatrixRMaj(0)), TEST_F64);

    }

    @Test
    public void testFMatrixSparseCSC() throws Exception {
        FMatrixSparseCSC expected = new FMatrixSparseCSC(18, 21, 10);
        expected.set(3, 8, rnd.nextFloat());
        expected.set(12, 18, rnd.nextFloat());
        expected.set(7, 9, rnd.nextFloat());
        expected.set(3, 6, rnd.nextFloat());
        expected.sortIndices(null);

        // Read CSC
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, expected.createLike()), TEST_F32);

        // Read Triplet
        FMatrixSparseTriplet triplet = saveAndLoad(expected, new FMatrixSparseTriplet());
        FMatrixSparseCSC actual = ConvertFMatrixStruct.convert(triplet, expected.createLike());
        EjmlUnitTests.assertEquals(expected, actual, TEST_F32);

        // Read Dense
        EjmlUnitTests.assertEquals(expected, saveAndLoad(expected, new FMatrixRMaj(0)), TEST_F32);

    }

    private <T extends org.ejml.data.Matrix> T saveAndLoad(org.ejml.data.Matrix matrix, T result) throws IOException {
        MatFile original = Mat5.newMatFile().addArray("matrix", asArray(matrix));
        return convert(writeReadMat(original).getMatrix("matrix"), result);
    }

    private MatFile writeReadMat(MatFile mat) throws IOException {
        // Debug to disk
        String name = "test/" + Thread.currentThread().getStackTrace()[3].getMethodName() + ".mat";
        try (Sink sink = Sinks.newStreamingFile(name)) {
            mat.writeTo(sink);
        }

        // Write to buffer and read result
        ByteBuffer buffer = ByteBuffer.allocate(sint32(mat.getUncompressedSerializedSize()));
        mat.writeTo(Sinks.wrap(buffer));
        buffer.flip();
        return Mat5.newReader(Sources.wrap(buffer)).readMat();
    }

    final Random rnd = new Random(0);

    static final int rows = 17;
    static final int cols = 7;

    private static void fillData(double[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
    }

    private static void fillData(float[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
    }

}
