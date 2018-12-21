/*-
 * #%L
 * MAT File Library
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.util.IndentingAppendable;
import us.hebi.matlab.mat.types.Source;
import us.hebi.matlab.mat.types.Sources;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Stack;

import static us.hebi.matlab.mat.util.Preconditions.*;

/**
 * Iterates through all tags in a MAT 5 file without trying to
 * decode them. Useful for debugging the actual file structure.
 *
 * @author Florian Enner
 * @since 03 Sep 2018
 */
class Mat5TagStreamer {

    Mat5TagStreamer(Source source) {
        this.source = checkNotNull(source);
    }

    Mat5TagStreamer setReducedHeader(boolean reduced) {
        this.reduced = reduced;
        return this;
    }

    private boolean reduced = false;
    private TagConsumer consumer;
    private final Source source;
    private boolean nextIsSubsys = false;

    void printTags() throws IOException {
        printTags(System.out);
    }

    void printTags(Appendable appendable) throws IOException {
        forEach(new TagPrinter(appendable));
    }

    void forEach(TagConsumer consumer) throws IOException {
        this.consumer = consumer;

        // Read file header
        source.order(ByteOrder.nativeOrder());
        Mat5File header = reduced ? Mat5File.readReducedFileHeader(source) : Mat5File.readFileHeader(source);
        source.order(header.getByteOrder());
        consumer.onFileStart(header);

        // Read root entries
        int numEntries = 0;
        boolean eof = false;
        while (!eof) {

            // Check if we're at the subsystem
            numEntries++;
            long position = source.getPosition();
            nextIsSubsys = reduced ? numEntries == 2 : (position == header.getSubsysOffset());

            // Read tag
            try {
                handleTag(Mat5Tag.readTag(source), source, position);
            } catch (EOFException ex) {
                consumer.onFileEnd();
                eof = true;
            }

        }

    }

    private void handleTag(Mat5Tag tag, Source source, long position) throws IOException {
        switch (tag.getType()) {
            case Matrix:
                handleMatrixTag(source, position, tag.getNumBytes());
                break;
            case Compressed:
                handleCompressedTag(source, position, tag.getNumBytes());
                break;
            default:
                if (nextIsSubsys && tag.getType() == Mat5Type.UInt8) {
                    // Subsys is stored as an Int8 vector with MAT-file like contents
                    if (consumer.onSubsystemBegin(position, tag.getNumBytes())) {
                        Source subsys = Sources.wrap(tag.readAsBytes());
                        new Mat5TagStreamer(subsys)
                                .setReducedHeader(true)
                                .forEach(consumer);
                        consumer.onSubsystemEnd();
                        nextIsSubsys = false;
                    } else {
                        nextIsSubsys = false;
                        handleTag(tag, source, position);
                    }
                } else {
                    // Normal data
                    if (!consumer.onData(position, tag))
                        source.skip(tag.getNumBytes() + tag.getPadding());
                }
        }
    }

    private void handleCompressedTag(Source source, long position, int numBytes) throws IOException {
        consumer.onCompressedBegin(position, numBytes);
        Source inflated = source.readInflated(numBytes, 2048);
        try {
            handleTag(Mat5Tag.readTag(inflated), inflated, 0);
        } finally {
            inflated.close();
        }
        consumer.onCompressedEnd();
    }

    private void handleMatrixTag(Source source, long position, int numBytes) throws IOException {
        consumer.onMatrixBegin(position, numBytes);
        long end = position + numBytes;
        long currentPos = source.getPosition();
        while (currentPos < end) {
            handleTag(Mat5Tag.readTag(source), source, currentPos);
            currentPos = source.getPosition();
        }
        consumer.onMatrixEnd();
    }

    interface TagConsumer {

        void onFileStart(Mat5File fileHeader) throws IOException;

        void onCompressedBegin(long position, int numBytes) throws IOException;

        void onCompressedEnd() throws IOException;

        void onMatrixBegin(long position, int numBytes) throws IOException;

        void onMatrixEnd() throws IOException;

        void onFileEnd() throws IOException;

        /**
         * @return true if subsystem should be read as well
         */
        boolean onSubsystemBegin(long position, int numBytes) throws IOException;

        void onSubsystemEnd() throws IOException;

        /**
         * @return true if data was consumed
         */
        boolean onData(long position, Mat5Tag tag) throws IOException;

    }

    static class TagPrinter implements TagConsumer {

        public TagPrinter(Appendable out) {
            this.out = new IndentingAppendable(out);
        }

        @Override
        public void onFileStart(Mat5File fileHeader) throws IOException {
            out.append(String.valueOf(fileHeader));
        }

        @Override
        public void onCompressedBegin(long position, int numBytes) throws IOException {
            out.append("\n")
                    .append("[").append(Integer.toString(count)).append("] ")
                    .append(Mat5Type.Compressed.name()).append(" (").append(String.valueOf(numBytes)).append(" bytes,")
                    .append(" position = ").append(Long.toString(position)).append(")");
            indent();
        }

        @Override
        public void onCompressedEnd() {
            unindent();
        }

        @Override
        public void onMatrixBegin(long position, int numBytes) throws IOException {
            out.append("\n")
                    .append("[").append(Integer.toString(count)).append("] ")
                    .append(Mat5Type.Matrix.name()).append(" (").append(String.valueOf(numBytes)).append(" bytes,")
                    .append(" position = ").append(Long.toString(position)).append(")");
            indent();
            nextIsArrayFlags = true;

        }

        @Override
        public void onMatrixEnd() {
            unindent();
            nextIsArrayFlags = false;
        }

        @Override
        public boolean onSubsystemBegin(long position, int numBytes) throws IOException {
            out.append("\n--- Begin Subsystem ---").append("\n");
            indent();
            return true;
        }

        @Override
        public void onSubsystemEnd() throws IOException {
            unindent();
        }

        @Override
        public void onFileEnd() throws IOException {
            out.append("\n--- End of File ---");
        }

        private void indent() {
            out.indent();
            prevCount.push(count);
            count = 0;
        }

        private void unindent() {
            out.unindent();
            count = prevCount.pop() + 1;
        }

        @Override
        public boolean onData(long position, Mat5Tag tag) throws IOException {
            out.append("\n")
                    .append("[").append(Integer.toString(count)).append("] ")
                    .append(tag.getType().name())
                    .append("[")
                    .append(Integer.toString(tag.getNumElements()))
                    .append("] = ");

            switch (tag.getType()) {
                case Int16:
                case UInt16:
                    out.append(Arrays.toString(tag.readAsShorts()));
                    break;

                case Int32:
                case UInt32:
                    int[] data = tag.readAsInts();
                    out.append(Arrays.toString(data));
                    if (nextIsArrayFlags && data.length == 2) {
                        out.append(" // ").append(Mat5ArrayFlags.getType(data).name());
                    }
                    break;

                case Int64:
                case UInt64:
                    out.append(Arrays.toString(tag.readAsLongs()));
                    break;

                case Single:
                    out.append(Arrays.toString(tag.readAsFloats()));
                    break;

                case Double:
                    out.append(Arrays.toString(tag.readAsDoubles()));
                    break;

                case Int8:
                    // Usually Int8 represents Ascii fields, so make the default more readable
                    byte[] bytes = tag.readAsBytes();
                    out.append("['").append(CharEncoding.parseAsciiString(bytes)).append("']");
                    break;

                default:
                    out.append(Arrays.toString(tag.readAsBytes()));

            }

            nextIsArrayFlags = false;
            count++;
            return true;
        }

        private boolean nextIsArrayFlags = false;
        private int count = 0;
        Stack<Integer> prevCount = new Stack<Integer>();
        private final IndentingAppendable out;

    }

}
