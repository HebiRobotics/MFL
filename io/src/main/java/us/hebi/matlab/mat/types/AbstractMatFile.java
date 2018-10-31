package us.hebi.matlab.mat.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Basic implementation for keeping track of the contents of a MAT file
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 04 May 2018
 */
public abstract class AbstractMatFile extends AbstractMatFileBase {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Array> T getArray(String name) {
        // Fast lookup (case sensitive)
        Array array = lookup.get(name);
        if (array != null)
            return (T) array;

        // Slow fallback (not case sensitive)
        for (NamedArray entry : entries) {
            if (name.equalsIgnoreCase(entry.getName()))
                return (T) entry.getValue();
        }

        throw new IllegalArgumentException("Could not find array: " + name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Array> T getArray(int index) {
        return (T) entries.get(index).getValue();
    }

    @Override
    public MatFile addArray(String name, Array value) {
        return addArray(new NamedArray(name, value));
    }

    @Override
    public MatFile addArray(NamedArray entry) {
        entries.add(entry);
        lookup.put(entry.getName(), entry.getValue());
        return this;
    }

    @Override
    public String toString() {
        return StringHelper.toString(entries);
    }

    /**
     * Closes all contained arrays, and throws an
     * IOException if any of them failed to close.
     */
    @Override
    public void close() throws IOException {
        IOException lastError = null;
        for (NamedArray entry : entries) {
            try {
                entry.getValue().close();
            } catch (IOException ioe) {
                lastError = ioe;
            }
        }
        entries.clear();
        if (lastError != null)
            throw lastError;
    }

    @Override
    public Iterator<NamedArray> iterator() {
        return entries.iterator();
    }

    @Override
    public int size() {
        return entries.size();
    }

    protected final HashMap<String, Array> lookup = new HashMap<String, Array>();
    protected final List<NamedArray> entries = new ArrayList<NamedArray>();

}
