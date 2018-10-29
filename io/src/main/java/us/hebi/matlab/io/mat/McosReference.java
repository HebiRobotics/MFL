package us.hebi.matlab.io.mat;

import us.hebi.matlab.io.types.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static us.hebi.matlab.io.mat.Mat5WriteUtil.*;

/**
 * Reference for handle classes, 'table', 'string', etc.
 * <p>
 * To the system it looks like a regular Object, but it is serialized
 * as an Opaque Object, i.e., single dimension element that contains
 * data about what it should look like.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 05 Sep 2018
 */
class McosReference extends AbstractStructBase implements ObjectStruct, Opaque, Mat5Serializable {

    /**
     * Handle references that live inside the subsystem show up with just the UInt32 content
     */
    static McosReference parseMcosReference(Array content) {
        if (!isMcosReference(content))
            throw new IllegalArgumentException("Not a valid reference");
        return parseOpaque(false, "MCOS", "N/A", content);
    }

    /**
     * Checks the UInt32 content for valid headers etc.
     */
    static boolean isMcosReference(Array content) {
        // Check that type matches
        if (content.getType() != MatlabType.UInt32)
            return false;

        // Check that dimensions are as expected
        if (content.getNumRows() < 5 || content.getNumCols() != 1)
            return false;

        // Check that the first two numbers (header) match expected values
        Matrix data = (Matrix) content;
        return data.getInt(0) == 0xdd000000 && data.getInt(1) == 2;
    }

    /**
     * Handle references that live inside the main MAT file show up as Opaque arrays with
     * a UInt32 content
     */
    static McosReference parseOpaque(boolean isGlobal, String objectType, String className, Array content) {

        if ("FileWrapper__".equals(className))
            throw new IllegalArgumentException("Only for references. Not for FileWrapper__ class");

        // Check that dimensions and type match expected
        if (!"MCOS".equals(objectType) || !isMcosReference(content))
            throw new IllegalArgumentException("Unexpected MCOS object reference data type: " + content);

        // Get Dimensions
        Matrix data = (Matrix) content;
        int[] dims = new int[2];
        dims[0] = data.getInt(2); // rows
        dims[1] = data.getInt(3); // cols

        // Sanity check that dimension matches length
        int numElements = getNumElements(dims);
        if (numElements != data.getNumRows() - 5)
            throw new IllegalArgumentException("Different number of references than dimensions");

        // Get object ids (next to second to last value)
        int[] objectIds = new int[numElements];
        for (int i = 0; i < objectIds.length; i++) {
            objectIds[i] = data.getInt(i + 4);
        }

        // Last value: class id
        int classId = data.getInt(data.getNumRows() - 1);
        return new McosReference(dims, isGlobal, objectType, className, content, objectIds, classId);

    }

    private McosReference(int[] dims, boolean isGlobal, String objectType, String className, Array content, int[] objectIds, int classId) {
        super(dims, isGlobal);

        // Opaque fields (used for serialization)
        this.content = content;
        this.className = className;
        this.objectType = objectType;

        // Object references (used for user interaction)
        this.objectIds = objectIds;
        this.classId = classId;
        this.objects = new McosObject[objectIds.length];
        Arrays.fill(objects, McosObject.EMPTY);

    }

    protected void setReferences(List<McosObject> objects) {
        for (int i = 0; i < objectIds.length; i++) {
            this.objects[i] = objects.get(objectIds[i]);
        }
    }

    @Override
    public List<String> getFieldNames() {
        return isEmpty() ? Collections.<String>emptyList() : objects[0].getFieldNames();
    }

    @Override
    @SuppressWarnings("unchecked") // simplifies casting
    public <T extends Array> T get(String field, int index) {
        return (T) objects[index].get(field);
    }

    @Override
    public Struct set(String field, int index, Array value) {
        objects[index].set(field, value);
        return this;
    }

    @Override
    public Array[] remove(String field) {
        throw new IllegalStateException("Can't remove fields from Reference Objects.");
    }

    @Override
    public MatlabType getType() {
        return MatlabType.Object;
    }

    public String getObjectType() {
        return objectType;
    }

    @Override
    public String getPackageName() {
        return isEmpty() ? "" : objects[0].getPackageName();
    }

    public String getClassName() {
        return isEmpty() ? className : objects[0].getClassName();
    }

    public Array getContent() {
        return content;
    }

    @Override
    public int getMat5Size(String name) {
        return computeOpaqueSize(name, this);
    }

    @Override
    public void writeMat5(String name, Sink sink) throws IOException {
        writeOpaque(name, this, sink);
    }

    @Override
    public void close() throws IOException {
        content.close();
        // Note: backing objects get closed automatically when subsystem gets closed
    }

    @Override
    public String toString() {
        return super.toString() + " for '" + getClassName() + "'";
    }

    private boolean isEmpty() {
        return objects.length == 0 || objects[0] == McosObject.EMPTY;
    }

    final String objectType;
    final String className;
    final Array content;

    final int[] objectIds;
    final int classId;
    final McosObject[] objects;

}
