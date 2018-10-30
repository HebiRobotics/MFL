package us.hebi.matlab.io.types;

/**
 * Undocumented MAT file type that is used for miscellaneous
 * things like the references of handle types as well as
 * serialized Java objects.
 *
 * @author Florian Enner
 * @since 04 Sep 2018
 */
public interface Opaque extends Array {

    String getObjectType();

    String getClassName();

    Array getContent();

}
