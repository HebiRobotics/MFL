/*-
 * #%L
 * Mat-File IO
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

package us.hebi.matlab.mat.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Basic implementation for keeping track of the contents of a MAT file
 *
 * @author Florian Enner
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
        for (Entry entry : entries) {
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
        return addArray(name, false, value);
    }

    @Override
    public MatFile addArray(String name, boolean isGlobal, Array value) {
        return addEntry(new Entry(name, isGlobal, value));
    }

    @Override
    public MatFile addEntry(Entry entry) {
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
        for (Entry entry : entries) {
            try {
                entry.getValue().close();
            } catch (IOException ioe) {
                lastError = ioe;
            }
        }
        clear();
        if (lastError != null)
            throw lastError;
    }

    @Override
    public Iterable<Entry> getEntries() {
        return entries;
    }

    @Override
    public int getNumEntries() {
        return entries.size();
    }

    @Override
    public void clear() {
        lookup.clear();
        entries.clear();
    }

    protected final HashMap<String, Array> lookup = new HashMap<String, Array>();
    protected final List<Entry> entries = new ArrayList<Entry>();

    @Override
    public final int hashCode() {
        return 31 * subHashCode() + entries.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null) {
            return false;
        } else if (other.getClass().equals(this.getClass())) {
            AbstractMatFile otherFile = (AbstractMatFile) other;
            return subEqualsGuaranteedSameClass(other) && otherFile.entries.equals(entries);
        } else {
            return false;
        }
    }

    protected abstract int subHashCode();

    protected abstract boolean subEqualsGuaranteedSameClass(Object otherGuaranteedSameClass);
}
