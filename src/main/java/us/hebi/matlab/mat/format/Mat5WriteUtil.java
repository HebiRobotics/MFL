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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.util.Casts;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Opaque;
import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.zip.Deflater;

import static us.hebi.matlab.mat.util.Casts.*;
import static us.hebi.matlab.mat.format.Mat5Type.Int32;
import static us.hebi.matlab.mat.format.Mat5Type.Int8;
import static us.hebi.matlab.mat.format.Mat5Type.*;
import static us.hebi.matlab.mat.format.Mat5Type.UInt32;
import static us.hebi.matlab.mat.types.MatlabType.*;

/**
 * Static utility methods for serializing custom classes
 *
 * @author Florian Enner
 * @since 26 Oct 2018
 */
public class Mat5WriteUtil {

    public static void writeArrayWithTag(Array array, Sink sink) throws IOException {
        writeArrayWithTag("", array, sink);
    }

    public static void writeArrayWithTag(String name, Array array, Sink sink) throws IOException {
        if (array instanceof Mat5Serializable) {
            ((Mat5Serializable) array).writeMat5(name, sink);
            return;
        }
        throw new IllegalArgumentException("Array does not support the MAT5 format");
    }

    public static int computeArraySize(Array array) {
        return computeArraySize("", array);
    }

    public static int computeArraySize(String name, Array array) {
        if (array instanceof Mat5Serializable)
            return ((Mat5Serializable) array).getMat5Size(name);
        throw new IllegalArgumentException("Array does not support the MAT5 format");
    }

    public static int computeArrayHeaderSize(String name, Array array) {
        int arrayFlags = UInt32.computeSerializedSize(2);
        int dimensions = Int32.computeSerializedSize(array.getNumDimensions());
        int nameLen = Int8.computeSerializedSize(name.length()); // ascii
        return arrayFlags + dimensions + nameLen;
    }

    public static void writeMatrixTag(String name, Mat5Serializable array, Sink sink) throws IOException {
        Matrix.writeTag(array.getMat5Size(name) - Mat5.MATRIX_TAG_SIZE, sink);
    }

    public static void writeArrayHeader(String name, Array array, Sink sink) throws IOException {
        if (array.getType() == Opaque)
            throw new IllegalArgumentException("Opaque types do not share the same format as other types");

        // Subfield 1: Meta data
        UInt32.writeIntsWithTag(Mat5ArrayFlags.forArray(array), sink);

        // Subfield 2: Dimensions
        Int32.writeIntsWithTag(array.getDimensions(), sink);

        // Subfield 3: Name
        Int8.writeBytesWithTag(name.getBytes(Charsets.US_ASCII), sink);
    }

    public static int computeOpaqueSize(String name, us.hebi.matlab.mat.types.Opaque array) {
        return Mat5.MATRIX_TAG_SIZE
                + UInt32.computeSerializedSize(2)
                + Int8.computeSerializedSize(name.length())
                + Int8.computeSerializedSize(array.getObjectType().length())
                + Int8.computeSerializedSize(array.getClassName().length())
                + computeArraySize(array.getContent());
    }

    public static void writeOpaque(String name, Opaque opaque, Sink sink) throws IOException {
        // Tag
        final int numBytes = computeOpaqueSize(name, opaque) - Mat5.MATRIX_TAG_SIZE;
        Matrix.writeTag(numBytes, sink);

        // Subfield 1: Meta data
        UInt32.writeIntsWithTag(Mat5ArrayFlags.forOpaque(opaque), sink);

        // Subfield 2: Ascii variable name
        Int8.writeBytesWithTag(name.getBytes(Charsets.US_ASCII), sink);

        // Subfield 3: Object Identifier (e.g. "MCOS", "handle", "java")
        Int8.writeBytesWithTag(opaque.getObjectType().getBytes(Charsets.US_ASCII), sink);

        // Subfield 4: Class name (e.g. "table", "string", "java.io.File")
        Int8.writeBytesWithTag(opaque.getClassName().getBytes(Charsets.US_ASCII), sink);

        // Subfield 5: Content
        writeArrayWithTag(opaque.getContent(), sink);
    }

    static int computeCharBufferSize(CharEncoding encoding, CharBuffer buffer) {
        Mat5Type tagType = Mat5Type.fromCharEncoding(encoding);
        int numElements = checkedDivide(encoding.getEncodedLength(buffer), tagType.bytes());
        return tagType.computeSerializedSize(numElements);
    }

    static void writeCharBufferWithTag(CharEncoding encoding, CharBuffer buffer, Sink sink) throws IOException {
        Mat5Type tagType = Mat5Type.fromCharEncoding(encoding);
        int numElements = checkedDivide(encoding.getEncodedLength(buffer), tagType.bytes());
        tagType.writeTag(numElements, sink);
        encoding.writeEncoded(buffer, sink);
        tagType.writePadding(numElements, sink);
    }

    static void writeArrayWithTagDeflated(String name, Array array, Sink sink, Deflater deflater) throws IOException {

        // Write placeholder tag with a dummy size so we can fill in info later
        long tagPosition = sink.position();
        Compressed.writeTag(DUMMY_SIZE, false, sink);
        long start = sink.position();

        // Compress matrix data
        Sink compressed = sink.writeDeflated(deflater);
        writeArrayWithTag(name, array, compressed);
        compressed.close(); // triggers flush/finish

        // Calculate actual size
        long end = sink.position();
        long compressedSize = end - start;

        // Overwrite placeholder tag with the real size
        sink.position(tagPosition);
        Compressed.writeTag(Casts.sint32(compressedSize), false, sink);

        // Return to the current real position after the compressed data
        // Note that compressed tags don't require padding for alignment.
        sink.position(end);

    }

    private static final int DUMMY_SIZE = 0;

}
