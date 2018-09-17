package us.hebi.matlab.io.types;

/**
 * Represents MATLAB's Object classes such as classdef
 * or handle types.
 * <p>
 * Objects in MATLAB can be accessed the same way as
 * structs, with the difference that users can't set
 * arbitrary fields. However, this is not enforced
 * in this library, as we don't know what fields are
 * allowed in the first place.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 Sep 2018
 */
public interface ObjectStruct extends Struct {

    String getPackageName();

    String getClassName();

}
