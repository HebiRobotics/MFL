package us.hebi.matlab.io.mat.mat5test;

import us.hebi.matlab.io.mat.Mat5;
import us.hebi.matlab.io.mat.Mat5File;
import us.hebi.matlab.io.mat.Mat5Writer;
import us.hebi.matlab.io.types.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Deflater;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 08 Sep 2018
 */
public class MatTestUtil {

    public static Mat5File readMat(String name) throws IOException {
        return readMat(name, testRoundTrip);
    }

    public static Mat5File readMat(String name, boolean testRoundTrip) throws IOException {
        return readMat(name, testRoundTrip, false);
    }

    public static Mat5File readMat(String name, boolean testRoundTrip, boolean reduced) throws IOException {
        // Read from original file
        Mat5File results = readMat(MatTestUtil.class, name, reduced);
        if (debugPrint) {
            System.out.println("source = " + results);
            for (NamedArray result : results) {
                System.out.println(result);
            }
        }

        if (!testRoundTrip)
            return results;

        // Write contents to buffer
        int errorPadding = 128;
        long maxSize = Mat5Writer.computeUncompressedSize(results) + errorPadding;
        ByteBuffer buffer = ByteBuffer.allocate((int) maxSize);
        Sink sink = Sinks.wrap(buffer);
        sink.setByteOrder(testOrder);
        try {

            Mat5.newWriter(sink)
                    .setDeflateLevel(deflateLevel)
                    .writeFile(results);

        } finally {
            sink.close();
        }

        // Read from new buffer
        buffer.flip();
        Mat5File file = Mat5.newReader(Sources.wrap(buffer)).setReducedHeader(reduced).readFile();
        if (debugPrint) {
            System.out.println("roundtrip = " + file);
            for (NamedArray result : file) {
                System.out.println(result);
            }
        }
        return file;

    }

    private static Mat5File readMat(Class location, String name, boolean reduced) throws IOException {
        Source source = getSource(location, name);
        try {
            return Mat5.newReader(source).setReducedHeader(reduced).readFile();
        } finally {
            source.close();
        }
    }

    private static Source getSource(Class clazz, String name) throws IOException {
        // Read mat into buffer
        InputStream inputStream = clazz.getResourceAsStream(name);
        checkNotNull(inputStream, "File %s could not be found", name);
        ByteBuffer buffer = ByteBuffer.allocate(inputStream.available());
        int bytes = inputStream.read(buffer.array());
        if (bytes != buffer.array().length) {
            throw new AssertionError("Could not read full contents of " + name);
        }
        inputStream.close();

        // Wrap buffer as source
        return Sources.wrap(buffer);
    }

    private static boolean testRoundTrip = true; // enabled does read/write/read before checking the data
    static boolean debugPrint = false;
    private static ByteOrder testOrder = ByteOrder.nativeOrder();
    private static int deflateLevel = Deflater.BEST_SPEED;
    static double DELTA = 0; // should read exactly the same bits
    static float FLOAT_DELTA = 1E-6f;

}
