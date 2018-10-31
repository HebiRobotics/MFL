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

/**
 * Represents MATLAB's Object classes such as classdef
 * or handle types.
 * <p>
 * Objects in MATLAB can be accessed the same way as
 * structs, with the difference that users can't set
 * arbitrary fields. However, this is not enforced
 * in this library, as we don't know what fields are
 * allowed in the first place.
 *
 * @author Florian Enner
 * @since 06 Sep 2018
 */
public interface ObjectStruct extends Struct {

    String getPackageName();

    String getClassName();

}
