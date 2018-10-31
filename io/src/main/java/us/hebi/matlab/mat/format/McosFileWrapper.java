package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.mat.format.Mat5Reader.*;
import static us.hebi.matlab.mat.format.McosReference.*;

/**
 * Special class that lives in the Mat5 Subsystem and contains the actual
 * objects/values that back the reference classes.
 * <p>
 * The format is unfortunately not documented in the official spec. This
 * implementation is based on the reverse engineering efforts by Matt Bauman
 * (http://nbviewer.jupyter.org/gist/mbauman/9121961) as well as the
 * implementation in JMatIO / MatFileRW by Matthew Dawson.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 05 Sep 2018
 */
class McosFileWrapper extends MatOpaque {

    McosFileWrapper(boolean isGlobal, String objectType, String className, Array content, ByteOrder order) {
        super(isGlobal, objectType, className, content);
        if (!"MCOS".equals(objectType))
            throw new IllegalArgumentException("Expected MCOS object type. Found " + objectType);
        if (!"FileWrapper__".equals(className))
            throw new IllegalArgumentException("Expected FileWrapper__ class. Found " + className);
        this.order = order;
    }

    @Override
    public Cell getContent() {
        return (Cell) super.getContent();
    }

    List<McosObject> parseObjects(McosRegistry mcosRegistry) throws IOException {
        this.mcosRegistry = checkNotNull(mcosRegistry);

        // Init various sources
        Matrix mcos = getContent().get(0);
        if (mcos.getType() != MatlabType.UInt8)
            throw readError("Unexpected MCOS data type. Expected: %s, Found: %s", MatlabType.UInt8, mcos.getType());

        ByteBuffer buffer = Mat5.exportBytes(mcos);
        buffer.order(order);

        // Version
        version = buffer.getInt();
        if (version != 2)
            throw readError("MAT file's MCOS data has an unknown version. Expected: %d, Found %d", 2, version);

        // String count
        int numStrings = buffer.getInt();

        // Get indices into the 5 segments
        segmentIndices = new int[6];
        for (int i = 0; i < segmentIndices.length; i++) {
            segmentIndices[i] = buffer.getInt();
        }

        // Make sure that last index ends at buffer end
        if (segmentIndices[5] != buffer.limit())
            throw readError("Unexpected end of segment 5. Expected: %d, Found: %d", buffer.limit(), segmentIndices[5]);

        // There should be 8 zero bytes. Make sure this is the case to avoid object format changes
        checkUnknown(buffer.getLong(), 0);

        // Finally, read in each string.
        strings = parseStrings(buffer, numStrings);

        // Sanity check, next 8 byte aligned position in the buffer should equal the start of the first segment!
        if (((buffer.position() + 0x07) & ~0x07) != segmentIndices[0]) {
            throw new IllegalStateException("Data from the strings section was not all read!");
        }

        // Parse individual segments
        classInfo = parseSegment1(buffer);
        objectInfo = parseSegment3(buffer);
        segment2Properties = parseSegment2(buffer);
        segment4Properties = parseSegment4(buffer);
        // parseSegment5(buffer); // Unknown what's in there

        return createObjects();
    }

    /**
     * @return List of reference-able objects containing the actual data
     */
    private List<McosObject> createObjects() {
        // The last item in the cell array is another cell array that contains a
        // struct for each class. Each struct contains (shared/default) properties that
        // are shared between instances. Note that these shared properties are not
        // related to class property defaults.
        Cell sharedProperties = getContent().get(getContent().getNumElements() - 1);

        List<McosObject> objects = new ArrayList<McosObject>(objectInfo.size());
        for (int i = 0; i < objectInfo.size(); i++) {

            // Initialize class name
            ObjectInfo objInfo = objectInfo.get(i);
            ClassInfo info = classInfo.get(objInfo.classId - 1);
            McosObject object = new McosObject(info.packageName, info.className);

            // Initialize all object fields to their shared/default values
            Struct shared = sharedProperties.get(objInfo.classId);
            for (String name : shared.getFieldNames()) {
                object.set(name, shared.get(name));
            }

            // Merge segment 2 properties
            if (objInfo.segment2PropertiesIndex > 0) {
                for (Property property : segment2Properties.get(objInfo.segment2PropertiesIndex - 1)) {
                    object.set(property.name, property.value);
                }
            }

            // Merge segment 4 properties
            if (objInfo.segment4PropertiesIndex > 0) {
                for (Property property : segment4Properties.get(objInfo.segment4PropertiesIndex - 1)) {
                    object.set(property.name, property.value);
                }
            }

            // Add to object list
            objects.add(object);

        }

        return objects;

    }

    private static String[] parseStrings(ByteBuffer buffer, int numStrings) {
        // Java doesn't provide an easy way to do this in bulk,
        // so just use a stupid formula for now.
        String[] strings = new String[numStrings];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; ++i) {
            sb.setLength(0);
            char next = (char) buffer.get();
            while (next != '\0') {
                sb.append(next);
                next = (char) buffer.get();
            }
            strings[i] = sb.toString();
        }
        return strings;
    }

    private String getString(int index) {
        // Indices are 1 indexed
        return index > 0 ? strings[index - 1] : "";
    }

    /**
     * Segment 1: Class information
     * <p>
     * "The first demarcated segment seems to describe the class information. I've not managed
     * to save fancy enough classes that expose all of these fields, but it at least enumerates
     * the classes, their names, and their package names (using the indexes into that heap of
     * strings)."
     */
    private List<ClassInfo> parseSegment1(ByteBuffer buffer) {
        buffer.position(segmentIndices[0]);

        // There are 16 unknown bytes. Make sure they are 0.
        checkUnknown(buffer.getLong(), 0);
        checkUnknown(buffer.getLong(), 0);

        List<ClassInfo> classNames = new ArrayList<ClassInfo>();
        while (buffer.position() < segmentIndices[1]) {

            String packageName = getString(buffer.getInt());
            String className = getString(buffer.getInt());
            checkUnknown(buffer.getLong(), 0);

            classNames.add(new ClassInfo(packageName, className));
        }

        // Sanity check that we finished this segment
        if (buffer.position() != segmentIndices[1]) {
            throw new IllegalStateException("Data from the class section was not all read!");
        }

        return classNames;

    }

    /**
     * Segment 2: Object properties that contain other objects
     * <p>
     * "The second segment is only sometimes there (e.g. offsets[2] == offsets[3]). When it is,
     * it contains informations about each object's properties. Each set has a variable number
     * of subelements, one for each property."
     */
    private List<Property[]> parseSegment2(ByteBuffer buffer) {
        return parseProperties(buffer, segmentIndices[1], segmentIndices[2]);
    }

    private List<Property[]> parseProperties(ByteBuffer buffer, int start, int end) {
        if (start == end)
            return Collections.emptyList();

        buffer.position(start);
        checkUnknown(buffer.getLong(), 0);

        List<Property[]> perClassProperties = new ArrayList<Property[]>();
        while (buffer.position() < end) {

            // For each class, there is an int describing the number of properties
            int numProps = buffer.getInt();
            Property[] properties = new Property[numProps];
            for (int i = 0; i < numProps; i++) {

                final String name = getString(buffer.getInt());
                final int flag = buffer.getInt();
                final int heapIndex = buffer.getInt();

                // The flag provides information on how the heapIndex should be used
                Array value;
                switch (flag) {
                    case 0:
                        // Stored in strings array
                        value = Mat5.newString(getString(heapIndex));
                        break;
                    case 1:
                        // Stored in FileWrapper__ heap (one indexed, but off by -3)
                        value = getContent().get(heapIndex + 2, 0);
                        break;
                    case 2:
                        // Property is a boolean, and the heapIndex itself is the value
                        value = Mat5.newLogicalScalar(heapIndex != 0);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected flag value: " + flag);
                }

                // The value may be a nested MCOS reference, in which case it may just
                // show up as UInt32 data without being contained in an Opaque object
                if (isMcosReference(value))
                    value = mcosRegistry.register(McosReference.parseMcosReference(value));

                // Add property
                properties[i] = new Property(name, value);

            }
            perClassProperties.add(properties);

            // Add padding if we've advanced by an uneven number of ints
            if ((numProps * 3 + 1) % 2 != 0)
                checkUnknown(buffer.getInt(), 0);

        }

        // Sanity check that we finished this segment
        if (buffer.position() != end) {
            throw new IllegalStateException("Data from the class section was not all read!");
        }

        return perClassProperties;

    }

    /**
     * Segment 3: Object information
     * <p>
     * "This section has one element per object. There is an index into the class structure, followed
     * by a few unknown fields. Then there are two fields that describe where the property information
     * is stored -- either in segment 2 or segment 4."
     */
    private List<ObjectInfo> parseSegment3(ByteBuffer buffer) {
        buffer.position(segmentIndices[2]);

        // The first 24 bytes are always zero. Perhaps reserved
        // space or an error due to 1 based indexing.
        checkUnknown(buffer.getLong(), 0);
        checkUnknown(buffer.getLong(), 0);
        checkUnknown(buffer.getLong(), 0);

        List<ObjectInfo> objectInfo = new ArrayList<ObjectInfo>();
        while (buffer.position() < segmentIndices[3]) {

            int classId = buffer.getInt();
            checkUnknown(buffer.getInt(), 0);
            checkUnknown(buffer.getInt(), 0);
            int segment2PropsIndex = buffer.getInt();
            int segment4PropsIndex = buffer.getInt();

            // Matt Bauman's document mentions that the following int is the 'objectId', but
            // this doesn't seem to be the case. While the number appears to look exactly like
            // an id in that it is unique and counts from 1 to the number of objects, the order
            // seems random and may or may not match the object id referenced by handle objects.
            // The handle classes seem to reference an index into the resulting list, so this
            // value doesn't seem to be needed at all.
            int supposedlyObjectId = buffer.getInt();

            objectInfo.add(new ObjectInfo(supposedlyObjectId, classId, segment2PropsIndex, segment4PropsIndex));

        }

        // Sanity check that we finished this segment
        if (buffer.position() != segmentIndices[3]) {
            throw new IllegalStateException("Data from the class section was not all read!");
        }

        return objectInfo;
    }

    /**
     * Segment 4: More properties!
     * <p>
     * "Just like segment 2, except these properties contain things that aren't class objects.
     * Strange that these two segments aren't adjacent..."
     */
    private List<Property[]> parseSegment4(ByteBuffer buffer) {
        return parseProperties(buffer, segmentIndices[3], segmentIndices[4]);
    }

    /**
     * Segment 5: Empty?
     * <p>
     * "I've never seen this populated, so I have no idea what is going on here."
     */
    private List<Object> parseSegment5(ByteBuffer buffer) {
        if (segmentIndices[4] == segmentIndices[5])
            return Collections.emptyList();

        // TODO: sometimes this is populated, e.g., in "plot-simple.fig"
        // TODO: Maybe check how to reproduce and what this is?
        byte[] bytes = new byte[segmentIndices[5] - segmentIndices[4]];
        buffer.position(segmentIndices[4]);
        buffer.get(bytes);
        System.out.println("\nSegment 5:\n" + Arrays.toString(bytes));
        throw new IllegalStateException("Segment 5 has data!");
    }

    private static void checkUnknown(long value, long expected) {
        if (value != expected)
            throw new IllegalStateException("MAT file's MCOS data has different byte values for unknown fields!  Aborting!");
    }

    private static class ClassInfo {
        ClassInfo(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        final String packageName;
        final String className;
    }

    private static class ObjectInfo {
        ObjectInfo(int objectId, int classId, int segment2PropertiesIndex, int segment4PropertiesIndex) {
            this.objectId = objectId;
            this.classId = classId;
            this.segment2PropertiesIndex = segment2PropertiesIndex;
            this.segment4PropertiesIndex = segment4PropertiesIndex;
        }

        final int objectId;
        final int classId;
        final int segment2PropertiesIndex;
        final int segment4PropertiesIndex;
    }

    private static class Property {
        Property(String name, Array value) {
            this.name = name;
            this.value = value;
        }

        final String name;
        final Array value;
    }

    // Member variables that get initialized in parseObjects()
    private final ByteOrder order;
    int version = -1;
    String[] strings = null;
    int[] segmentIndices = null;
    List<ClassInfo> classInfo = null;
    List<ObjectInfo> objectInfo = null;
    List<Property[]> segment2Properties = null;
    List<Property[]> segment4Properties = null;
    McosRegistry mcosRegistry = null;

}
