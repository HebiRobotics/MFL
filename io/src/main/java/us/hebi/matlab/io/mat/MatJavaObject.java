package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 01 Sep 2018
 */
class MatJavaObject extends MatOpaque implements JavaObject {

    MatJavaObject(boolean isGlobal, String className, Array content) {
        super(isGlobal, "java", className, content);
    }

    /**
     * Finds the binary data where the serialized bytes are stored.
     * Note: Logic copied from MatFileRW
     *
     * @return Array containing the serialized bytes
     */
    private Matrix getSerializedData() {
        Array content = getContent();

        // Usually returned directly
        if (content instanceof Matrix)
            return (Matrix) content;

        // Sometimes returned in a structure field
        if (content instanceof Struct)
            return ((Struct) content).get("Values");

        // Sometimes stored as a cell. In that case we'll
        // take the first numeric array we can find.
        if (content instanceof Cell) {
            Cell cells = (Cell) content;
            for (int i = 0; i < cells.getNumElements(); i++) {
                Array array = cells.get(i);
                if (array instanceof Matrix) {
                    return (Matrix) array;
                }
            }
        }

        String msg = String.format("Unexpected byte storage. Found: %s", content);
        throw new IllegalStateException(msg);
    }

    @Override
    public Object instantiateObject() throws Exception {
        // Export matrix as raw bytes (can be uint8/int8/uint32)
        ByteBuffer buffer = Mat5.exportBytes(getSerializedData());
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // Convert to input stream
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

}
