package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.Sink;

import java.io.IOException;

/**
 * Marker interface for all parts (e.g. array/header/data components) that can be serialized using
 * the MAT5 format. Arrays that do not implement this interface may be rejected by the Mat5Writer.
 * <p>
 * The serialized bytes include any necessary tags (e.g. matrix tag) and padding
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 29 Aug 2018
 */
public interface Mat5Serializable {

    /**
     * @param name
     * @return Number of serialized bytes including the Matrix tag
     */
    int getMat5Size(String name);

    void writeMat5(String name, Sink sink) throws IOException;

    /**
     * Interface for serializing custom array classes that need to
     * have control over array flags/attributes without implementing
     * the entire interface (e.g. sparse::getNzMax() or matrix::isComplex())
     */
    interface Mat5Attributes {

        boolean isLogical();

        boolean isGlobal();

        boolean isComplex();

        int getNzMax();

    }

}
