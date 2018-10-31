package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains object data that can be referenced. Each object only
 * represents a single instance, i.e., a reference array would
 * contain multiple such objects. The property map forms automatically
 * and in the same order as the properties were added.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 Sep 2018
 */
class McosObject {

    McosObject(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    String getPackageName() {
        return packageName;
    }

    String getClassName() {
        return className;
    }

    List<String> getFieldNames() {
        return fieldNames;
    }

    void set(String name, Array value) {
        if (properties.put(name, value) == null) {
            fieldNames.add(name);
        }
    }

    Array get(String name) {
        return properties.get(name);
    }

    private final String packageName;
    private final String className;
    private final List<String> fieldNames = new ArrayList<String>(8);
    private final Map<String, Array> properties = new HashMap<String, Array>(16);

    final static McosObject EMPTY = new McosObject("", "") {
        @Override
        public void set(String name, Array value) {
            throw new IllegalStateException("Can't set empty reference.");
        }
    };

    @Override
    public String toString() {
        return "'" + getClassName() + "' class";
    }
}
