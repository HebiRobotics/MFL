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

package us.hebi.matlab.mat.format;

import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.util.Casts;

import java.io.Closeable;
import java.io.IOException;

/**
 * Provides a way to store numerical data with different types and
 * can be thought of as an array.
 *
 * @author Florian Enner
 * @since 03 May 2018
 */
interface NumberStore extends Closeable {

    int getNumElements();

    long getLong(int index);

    void setLong(int index, long value);

    double getDouble(int index);

    void setDouble(int index, double value);

    int getMat5Size();

    void writeMat5(Sink sink) throws IOException;

    static int hashCodeForType(NumberStore store, boolean logical, MatlabType type) {
        if (store == null) {
            return 0;
        }
        int hash = 1;
        if (logical) {
            for (int i = 0; i < store.getNumElements(); ++i) {
                hash = 31 * hash + (Casts.logical(store.getDouble(i)) ? 1 : 0);
            }
        } else {
            if (type == MatlabType.Single || type == MatlabType.Double) {
                for (int i = 0; i < store.getNumElements(); ++i) {
                    hash = 31 * hash + Double.hashCode(store.getDouble(i));
                }
            } else {
                for (int i = 0; i < store.getNumElements(); ++i) {
                    hash = 31 * hash + Long.hashCode(store.getLong(i));
                }
            }
        }
        return hash;
    }

    static boolean equalForType(NumberStore a, NumberStore b, boolean logical, MatlabType type) {
        if ((a == null) != (b == null)) {
            //  null mismatch is easy
            return false;
        } else if (a == null) {
            // they're both null
            return true;
        }
        if (a instanceof UniversalNumberStore && b instanceof UniversalNumberStore) {
            UniversalNumberStore aCast = (UniversalNumberStore) a;
            UniversalNumberStore bCast = (UniversalNumberStore) b;
            if (aCast.type == bCast.type) {
                // when their types are equal, then their binary content must be exactly the same (much faster)
                return aCast.buffer.equals(bCast.buffer);
            }
        }
        if (logical) {
            // compare floating-point types as doubles
            for (int i = 0; i < a.getNumElements(); ++i) {
                if (Casts.logical(a.getDouble(i)) != Casts.logical(b.getDouble(i))) {
                    return false;
                }
            }
        } else {
            // we need to compare without casting losses, so we need to know if these stores are being used
            // as floating points or integer types
            if (type == MatlabType.Single || type == MatlabType.Double) {
                // compare floating-point types as doubles
                for (int i = 0; i < a.getNumElements(); ++i) {
                    if (a.getDouble(i) != b.getDouble(i)) {
                        return false;
                    }
                }
            } else {
                // and integer types as ints
                for (int i = 0; i < a.getNumElements(); ++i) {
                    if (a.getLong(i) != b.getLong(i)) {
                        return false;
                    }
                }
            }
        }
        // same number of elements and numeric values, so they're equal
        return true;
    }

}
