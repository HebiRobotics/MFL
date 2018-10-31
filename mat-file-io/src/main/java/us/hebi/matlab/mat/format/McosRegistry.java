package us.hebi.matlab.mat.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of reference classes that require a backing object
 */
class McosRegistry {

    synchronized McosReference register(McosReference reference) {
        this.references.add(reference);
        return reference;
    }

    List<McosReference> getReferences() {
        return references;
    }

    private final List<McosReference> references = new ArrayList<McosReference>();

}
