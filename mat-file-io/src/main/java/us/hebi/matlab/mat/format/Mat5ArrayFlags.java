package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.format.Mat5Serializable.Mat5Attributes;
import us.hebi.matlab.mat.types.*;

/**
 * Utilities to deal with the int[2] array flags that are
 * at the beginning of each array.
 *
 * @author Florian Enner
 * @since 14 Sep 2018
 */
class Mat5ArrayFlags {

    static int[] forArray(Array array) {
        if (array instanceof Mat5Attributes) {
            Mat5Attributes attr = ((Mat5Attributes) array);
            return create(array.getType(), attr.isGlobal(), attr.isLogical(), attr.isComplex(), attr.getNzMax());
        }
        boolean logical = array instanceof Matrix && ((Matrix) array).isLogical();
        boolean complex = array instanceof Matrix && ((Matrix) array).isComplex();
        int nzMax = array instanceof Sparse ? ((Sparse) array).getNzMax() : 0;
        return Mat5ArrayFlags.create(array.getType(), array.isGlobal(), logical, complex, nzMax);
    }

    /**
     * Opaques may show up as a different public type, e.g., Object. This method ignores
     * the public type and forces the array type to be Opaque.
     */
    static int[] forOpaque(Opaque opaque) {
        return create(MatlabType.Opaque, opaque.isGlobal(), false, false, 0);
    }

    private static int[] create(MatlabType type, boolean global, boolean logical, boolean complex, int nzMax) {
        int attributes = type.id() & FLAG_MASK_TYPE_ID;
        if (logical) attributes |= FLAG_BIT_LOGICAL;
        if (global) attributes |= FLAG_BIT_GLOBAL;
        if (complex) attributes |= FLAG_BIT_COMPLEX;
        return new int[]{attributes, nzMax};
    }

    static MatlabType getType(int[] arrayFlags) {
        return MatlabType.fromId(arrayFlags[0] & FLAG_MASK_TYPE_ID);
    }

    static boolean isComplex(int[] arrayFlags) {
        return (arrayFlags[0] & FLAG_BIT_COMPLEX) != 0;
    }

    static boolean isGlobal(int[] arrayFlags) {
        return (arrayFlags[0] & FLAG_BIT_GLOBAL) != 0;
    }

    static boolean isLogical(int[] arrayFlags) {
        return (arrayFlags[0] & FLAG_BIT_LOGICAL) != 0;
    }

    static int getNzMax(int[] arrayFlags) {
        return arrayFlags[1];
    }

    private static final int FLAG_MASK_TYPE_ID = 0xff;
    private static final int FLAG_BIT_LOGICAL = 1 << 9;
    private static final int FLAG_BIT_GLOBAL = 1 << 10;
    private static final int FLAG_BIT_COMPLEX = 1 << 11;

}
