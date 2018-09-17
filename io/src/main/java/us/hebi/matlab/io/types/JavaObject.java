package us.hebi.matlab.io.types;

/**
 * Wraps the content of a serialized Java object.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 01 Sep 2018
 */
public interface JavaObject extends Opaque {

    /**
     * @return instantiates Java object using deserialization
     */
    Object instantiateObject() throws Exception;

}
