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

import us.hebi.matlab.mat.types.Sink;

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

}
