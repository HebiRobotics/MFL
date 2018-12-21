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

package us.hebi.matlab.mat.types;

import java.io.Closeable;

/**
 * Represents the 'miMATRIX' type that is used to represent
 * all kinds of named variables inside a MAT file. This includes
 * N-dimensional numerical arrays as well as aggregate types such
 * as structs or cell arrays.
 * <p>
 * Note that all MATLAB variables are implemented as N-dimensional arrays,
 * and that singular values or structs are arrays with 1 row and 1 column.
 *
 * @author Florian Enner
 * @since 02 May 2018
 */
public interface Array extends Closeable {

    MatlabType getType();

    int[] getDimensions();

    int getNumDimensions();

    int getNumRows();

    int getNumCols();

    int getNumElements();

}
