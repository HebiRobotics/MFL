package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.ObjectStruct;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 03 Sep 2018
 */
class MatObjectStruct extends MatStruct implements ObjectStruct {

    MatObjectStruct(int[] dims, boolean isGlobal, String className, String[] names, Array[][] values) {
        super(dims, isGlobal, names, values);
        this.className = className;
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Object;
    }

    @Override
    public String getPackageName() {
        return "";
    }

    @Override
    public String getClassName() {
        return className;
    }

    private final String className;

}
