package us.hebi.matlab.mat.tests.mat73;

import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;
import org.junit.Test;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.types.MatFile;
import us.hebi.matlab.mat.util.IndentingAppendable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Florian Enner
 * @since 18 Mai 2022
 */
public class Mat73Test {

    File baseDir = new File("..");
    File base50 = new File(baseDir, "/mfl-core/src/test/resources/us/hebi/matlab/mat/tests/mat5/arrays");
    File base73 = new File(baseDir, "/mfl-mat73/src/test/resources/us/hebi/matlab/mat/tests/mat73/arrays");

    @Test
    public void loadFile() throws IOException {
        String fileName = "sparse.mat";

        try (MatFile matFile = Mat5.readFromFile(new File(base50, fileName))) {
            System.out.println(matFile);
        }

        System.out.println(" ===================== ");

        IndentingAppendable out = new IndentingAppendable(System.out);
        try (HdfFile hdfFile = new HdfFile(new File(base73, fileName))) {
            System.out.println(hdfFile);

            for (Node node : hdfFile) {
                recursivePrint(node, out);
            }

        }

    }

    private void recursivePrint(Node node, IndentingAppendable out) {
        out.append(node);
        out.append(" (attributes: ");
        for (Map.Entry<String, Attribute> attribute : node.getAttributes().entrySet()) {
            out.append(attribute.getKey());
            out.append("=");
            out.append(attribute.getValue().getData());
            out.append(",");
        }
        out.append(")");

        if (node instanceof Dataset) {
            Dataset ds = (Dataset) node;
            out.append(" (").append(ds.getDataType()).append(", ").append(ds.getJavaType().getSimpleName()).append(')');
        }
        if (node.isGroup()) {
            out.indent();
            out.append("\n");
            Map<String, Node> children = ((Group) node).getChildren();
            for (Map.Entry<String, Node> child : children.entrySet()) {
                out.append(child.getKey()).append(": ");
                recursivePrint(child.getValue(), out);
            }
            out.unindent();
            out.append("\n");
            return;
        }
        out.append('\n');
    }

}
