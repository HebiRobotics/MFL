package us.hebi.matlab.common.util;

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
