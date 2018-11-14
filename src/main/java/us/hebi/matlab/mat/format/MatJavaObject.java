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

import us.hebi.matlab.mat.types.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * @author Florian Enner
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
