package us.hebi.matlab.mat.format;

import org.junit.Test;
import us.hebi.matlab.mat.format.mat5test.MatTestUtil;
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