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

import org.junit.Test;
import us.hebi.matlab.mat.tests.mat5.MatTestUtil;
import us.hebi.matlab.mat.types.Source;
import us.hebi.matlab.mat.types.Sources;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public class Mat5TagStreamerTest {

    @Test
    public void printJavaFileTags() throws Exception {
        InputStream inputStream = MatTestUtil.class.getResourceAsStream("arrays/java.mat");
        Source source = Sources.wrapInputStream(inputStream);
        StringBuilder out = new StringBuilder(4096);
        new Mat5TagStreamer(source)
                .setReducedHeader(false)
                .printTags(out);
        source.close();
        assertTrue(out.length() > 0);
    }

    @Test
    public void printMcosHandlesFileTags() throws Exception {
        InputStream inputStream = MatTestUtil.class.getResourceAsStream("mcos/handles.mat");
        Source source = Sources.wrapInputStream(inputStream);
        StringBuilder out = new StringBuilder(8192);
        new Mat5TagStreamer(source)
                .setReducedHeader(false)
                .printTags(out);
        source.close();
        assertTrue(out.length() > 0);
    }

}
