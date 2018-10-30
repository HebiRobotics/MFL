package us.hebi.matlab.io.types;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * Represents a root element, i.e., an array with a variable name.
 * <p>
 * Note that the MAT5 format technically allows names for nested arrays,
 * but in reality this is only useful for the root level. All nested arrays
 * should always have an empty name as structs store the name of each field,
 * and cell arrays don't use names at all.
 *
 * @author Florian Enner
 * @since 8 May 2018
 */
public final class NamedArray {

    public NamedArray(String name, Array value) {
        this.name = checkNotNull(name, "Name can't be null");
        this.value = checkNotNull(value, "Value can't be null");
    }

    public String getName() {
        return name;
    }

    public Array getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + " = " + value;
    }

    private final String name;
    private final Array value;

}
