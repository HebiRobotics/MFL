package us.hebi.matlab.io.types;

/**
 * Represents the user-facing class type that MATLAB would display the data as
 */
public enum MatlabType {

    Cell(1, "cell"), // mxCELL_CLASS
    Structure(2, "struct"),
    Object(3, "object"),
    Character(4, "char"),
    Sparse(5, "sparse"),
    Double(6, "double"),
    Single(7, "single"),
    Int8(8, "int8"),
    UInt8(9, "uint8"),
    Int16(10, "int16"),
    UInt16(11, "uint16"),
    Int32(12, "int32"),
    UInt32(13, "uint32"),
    Int64(14, "int64"),
    UInt64(15, "uint64"),

    // Undocumented classes
    Function(16, "function_handle"), // function handles
    Opaque(17, "opaque"); // e.g. MCOS, tables, java types, etc.

    @Override
    public String toString() {
        return name;
    }

    public static MatlabType fromId(int id) {
        if (id > 0 && id < lookup.length) {
            MatlabType type = lookup[id];
            if (type != null)
                return type;
        }
        throw new IllegalArgumentException("Unknown array type for id: " + id);
    }

    /**
     * @return MAT 5 id
     */
    public byte id() {
        return id;
    }

    MatlabType(int id, String name) {
        this.id = (byte) id;
        this.name = name;
    }

    private final byte id;
    private final String name;

    private static final MatlabType[] lookup;

    static {
        // Determine size of lookup table
        int highestId = 0;
        for (MatlabType type : values()) {
            highestId = Math.max(highestId, type.id);
        }

        // Populate lookup table
        lookup = new MatlabType[highestId + 1];
        for (MatlabType type : values()) {
            lookup[type.id()] = type;
        }
    }

}
