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

package us.hebi.matlab.mat.util;

import java.io.IOException;

/**
 * Keeps track of indentations and indents all newlines
 *
 * @author Florian Enner
 * @since 03 Sep 2018
 */
public class IndentingAppendable implements Appendable {

    public IndentingAppendable(Appendable appendable) {
        this.appendable = appendable;
    }

    public String getIndentString() {
        return indentString;
    }

    public void setIndentString(String indentString) {
        this.indentString = indentString;
    }

    public int getMaxLineWidth() {
        return maxLineWidth;
    }

    public void setMaxLineWidth(int maxLineWidth) {
        this.maxLineWidth = maxLineWidth;
    }

    public String getOverflowString() {
        return overflowString;
    }

    public void setOverflowString(String overflowString) {
        this.overflowString = overflowString;
    }

    public IndentingAppendable indent() {
        numIndents++;
        return this;
    }

    public IndentingAppendable unindent() {
        numIndents--;
        return this;
    }

    public IndentingAppendable append(Object object) {
        return append(String.valueOf(object));
    }

    @Override
    public IndentingAppendable append(CharSequence csq) {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public IndentingAppendable append(CharSequence csq, int start, int end) {
        for (int i = start; i < end; i++) {
            append(csq.charAt(i));
        }
        return this;
    }


    @Override
    public IndentingAppendable append(char c) {
        try {
            return append0(c);
        } catch (IOException ioe) {
            // Most outputs (StringBuilder, System.out, etc.)
            // don't throw exceptions, so remove them by default.
            throw new RuntimeException(ioe);
        }
    }

    private IndentingAppendable append0(char c) throws IOException {
        // Reset to new line
        if (c == '\n') {
            appendable.append(c);
            currentLine = 0;
            for (int i = 0; i < numIndents; i++) {
                append(indentString);
            }
            return this;
        }

        // Ignore outside of bounds
        if (currentLine >= maxLineWidth)
            return this;

        // Add concat string
        if (currentLine > maxLineWidth - overflowString.length()) {
            currentLine += overflowString.length();
            appendable.append(overflowString);
            return this;
        }

        // Standard add
        currentLine++;
        appendable.append(c);
        return this;
    }

    private String indentString = "  ";
    private int numIndents = 0;
    private int maxLineWidth = 160;
    private int currentLine = 0;
    private String overflowString = "...";
    private final Appendable appendable;

}
