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

/**
 * Represents MATLAB's sparse matrix types
 * Behavior:
 * - Always two dimensional
 * - May not be logical
 * - May be complex
 * - Data is internally always stored as double
 * <p>
 * Note that index based access works on the non-zero values,
 * so there are only nzMax values.
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface Sparse extends Matrix {

    /**
     * Returns the maximum number of non-zero elements. Note that this
     * typically is the same as the actual number of non-zero elements,
     * but for empty matrices this may be 1.
     * <p>
     * NOTE: This behavior is tied to the way empty sparse matrices are
     * stored in the MAT 5 format, so it may make sense to deprecate this
     * method and replace it with a `getNumNz()` method.
     *
     * @return maximum number of non-zero elements
     */
    int getNzMax();

    double getDefaultValue();

    void setDefaultValue(double value);

    /**
     * Performs the supplied action for each non-zero value
     */
    void forEach(SparseConsumer action);

    interface SparseConsumer {
        void accept(int row, int col, double real, double imaginary);
    }

}
