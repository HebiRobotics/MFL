package us.hebi.matlab.io.mat;

import us.hebi.matlab.common.util.Charsets;
import us.hebi.matlab.io.types.*;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * --- Level 5 MAT-File Header Format ---
 * Level 5 MAT-files are made up of a 128-byte header followed by one or more data elements.
 * The header is made up of a 124-byte (ascii) text field and two, 16-bit flag fields.
 * <p>
 * [116 byte descriptive text] ("MATLAB 5.0 MAT-file" + arbitrary text)
 * [8 byte Subsystem data offset]
 * [2 byte version] (always 1)
 * [2 byte endian indicator]
 * <p>
 * --- Header Text Field ---
 * The first 116 bytes of the header can contain text data in human-readable form. This text
 * typically provides information that describes how the MAT-file was created. For example,
 * MAT-files created by MATLAB include the following information in their headers:
 * - Level of the MAT-file (value equals 1 for Level 5)
 * - Platform on which the file was created
 * - Date and time the file was created
 * <p>
 * Example: "MATLAB 5.0 MAT-file, Platform: SOL2, Created on: Thu Nov 13 10:10:27 1997"
 * <p>
 * --- Subsystem Data Offset Field ---
 * Bytes 117 through 124 of the header contain an offset to subsystem-specific data in the
 * MAT-file. All zeros or all spaces in this field indicate that there is no subsystem-specific
 * data stored in the file.
 * <p>
 * NOTE: is this field ever used? So far this was always zero.
 * <p>
 * --- Version Field ---
 * Constant 0x0100
 * <p>
 * --- Endian Indicator ---
 * 'M' and 'I' are written as a short in the file order (MATLAB defaults to the system/native order).
 * If the short gets returned as 'IM' it indicates that the byte order needs to be swapped.
 *
 * <p>
 * {@see http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf}
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 30 Aug 2018
 */
public class Mat5File extends AbstractMatFile {

    public static Mat5File readFileHeader(Source source) throws IOException {
        return readFileHeader(source, false);
    }

    public static Mat5File readReducedFileHeader(Source source) throws IOException {
        return readFileHeader(source, true);
    }

    private static Mat5File readFileHeader(Source source, boolean reducedHeader) throws IOException {

        // "Reduced" Mat file headers don't contain a description or subsystem offset. They are used
        // for e.g. Simulink models as well as the (optional) subsystem inside normal MAT files.
        String description = "";
        long subsysOffset = 0;
        if (!reducedHeader) {
            // 116 bytes description string
            byte[] buffer = new byte[116];
            source.readBytes(buffer, 0, buffer.length);
            description = CharEncoding.parseAsciiString(buffer, 0, buffer.length);
            checkMat5Identifier(description);

            // 8 bytes offset to subsystem-specific data. All zeros or spaces means no subsystem.
            // Note: the subsystem has an undocumented format and stores information on, e.g.,
            // MATLAB classes (MCOS - MATLAB Class Object System).
            //
            // See http://nbviewer.jupyter.org/gist/mbauman/9121961#The-Matfile-subsystem-for-version-5.0
            subsysOffset = source.readLong();
        }

        // 2 byte version: note that this should be a constant '1', but always seems to
        // be written as 256. For example, MATLAB 2017b writes 0x0001 on a LE system even
        // though the spec says that it should always be 0x0100.
        short version = Short.reverseBytes(source.readShort());

        // 2 byte endian indicator
        ByteOrder order = source.getByteOrder();
        short endianIndicator = source.readShort();
        switch (endianIndicator) {
            case 'M' << 8 | 'I': // order is correct
                break;
            case 'I' << 8 | 'M': // order needs to be reversed
                order = reverseByteOrder(order);
                subsysOffset = Long.reverseBytes(subsysOffset);
                version = Short.reverseBytes(version);
                break;
            default:
                throw new IllegalArgumentException("Invalid endian indicator");
        }

        // Reduced headers don't end aligned, so we need to skip to next boundary
        if (reducedHeader) {
            source.skip(4);
        }

        // Create file with header info
        return new Mat5File(reducedHeader, description, subsysOffset, order, version);
    }

    public void writeFileHeader(Sink sink) throws IOException {
        if (!hasReducedHeader()) {

            // 116 bytes description string
            checkMat5Identifier(getDescription());
            byte[] bytes = getDescription().getBytes(Charsets.US_ASCII);
            int remaining = 116 - bytes.length;
            sink.writeBytes(bytes, 0, Math.min(116, bytes.length));

            if (remaining > 0) {
                // fill remaining space
                byte[] buffer = new byte[remaining];
                Arrays.fill(buffer, (byte) '\0');
                sink.writeBytes(buffer, 0, buffer.length);
            } else if (remaining < 0) {
                String msg = "Warning: Description is " + (-remaining) + " bytes too long and will be concatenated.";
                System.err.println(msg);
            }

            // 8 bytes offset to subsystem
            sink.writeLong(getSubsysOffset());

        }

        // 2 byte version. Note that this should be a constant '1', but
        // always seems to be written as 256.
        sink.writeShort(Short.reverseBytes(getVersion()));

        // 2 byte endian indicator
        sink.writeShort((short) ('M' << 8 | 'I'));

        // Padding
        if (hasReducedHeader()) {
            sink.writeInt(0);
        }

    }

    public void updateSubsysOffset(long headerStart, long subsysStart, Sink sink) throws IOException {
        // Not necessary for reduced
        if (hasReducedHeader())
            return;

        // Jump back to beginning and overwrite offset
        long currentPosition = sink.position();
        sink.position(headerStart + 116);
        sink.writeLong(subsysStart - headerStart);
        sink.position(currentPosition);
    }

    /**
     * Optional part (without identifier) of the description text in a format that
     * closely matches MATLAB generated files
     * <p>
     * MATLAB (R2017b): ", Platform: PCWIN64, Created on: Sat Sep  1 09:58:17 2018"
     * Java: ", Platform: Windows 8.1, Created on: Sat Sep 01 09:59:06 CEST 2018"
     *
     * @return description
     */
    public static String getDefaultDescription() {
        return ", Platform: " + System.getProperty("os.name") + ", Created on: " + new Date().toString();
    }

    protected Mat5File() {
        this(getDefaultDescription());
    }

    protected Mat5File(String description) {
        this.description = MAT5_IDENTIFIER + description;
        subsysOffset = 0;
        byteOrder = ByteOrder.nativeOrder();
        version = 1;
        reduced = false;
    }

    private Mat5File(boolean reduced, String description, long subsysOffset, ByteOrder byteOrder, short version) {
        this.reduced = reduced;
        this.description = description;
        this.subsysOffset = subsysOffset;
        this.byteOrder = byteOrder;
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public long getSubsysOffset() {
        return subsysOffset;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public short getVersion() {
        return version;
    }

    public boolean hasReducedHeader() {
        return reduced;
    }

    @Override
    public String toString() {
        return "Mat5File{" +
                "description='" + description + '\'' +
                ", subsysOffset=" + subsysOffset +
                ", byteOrder=" + byteOrder +
                ", version=" + version +
                "}\n" + super.toString();
    }

    @Override
    public long getUncompressedSerializedSize() {
        return Mat5Writer.computeUncompressedSize(this);
    }

    @Override
    public void writeTo(Sink sink) throws IOException {
        new Mat5Writer(sink).writeFile(this);
    }

    public Mat5Subsystem getSubsystem() {
        Array last = getLast();
        return last instanceof Mat5Subsystem ? (Mat5Subsystem) last : null;
    }

    public Mat5File addArray(NamedArray entry) {
        if (reduced && size() >= 2)
            throw new IllegalStateException("Reduced MAT 5 files may not contain more than 2 entries");

        // Make sure that the optional subsystem is
        // located at the end of the file
        if (getLast() instanceof Mat5Subsystem) {
            entries.add(size() - 1, entry);
        } else {
            entries.add(entry);
        }
        lookup.put(entry.getName(), entry.getValue());
        return this;
    }

    private Array getLast() {
        return size() == 0 ? null : entries.get(size() - 1).getValue();
    }

    private final String description;
    private final long subsysOffset;
    private final ByteOrder byteOrder;
    private final short version;
    private final boolean reduced;

    private static void checkMat5Identifier(String description) {
        if (!description.startsWith(MAT5_IDENTIFIER)) {
            throw new IllegalArgumentException("This is not a valid MATLAB 5.0 MAT-file description.");
        }
    }

    private static final String MAT5_IDENTIFIER = "MATLAB 5.0 MAT-file";

}
