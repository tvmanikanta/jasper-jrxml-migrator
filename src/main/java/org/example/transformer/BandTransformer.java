package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Band Migration:
 *
 * 1. splitType attribute:
 *    - Old values: "Stretch", "Prevent", "Immediate"
 *    - JR7 values: "Stretch", "Prevent", "Immediate" (same, but default changed from Stretch → Prevent)
 *    - When no splitType is present, add splitType="Stretch" to preserve JR6 behaviour.
 *
 * 2. <band height="0"> — zero-height bands with no children can be collapsed (warn only).
 *
 * 3. isSplitAllowed (very old — pre JR5) → splitType="Prevent" or splitType="Stretch"
 */
public class BandTransformer extends AbstractJrxmlTransformer {

    private static final Set<String> BAND_NAMES = new HashSet<>(Arrays.asList(
            "background", "title", "pageHeader", "columnHeader",
            "detail", "columnFooter", "pageFooter", "lastPageFooter",
            "summary", "noData"
    ));

    @Override public String getName() { return "Band"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            // band element is nested under section names or directly under root
            if ("band".equals(el.getName())) {
                changes.addAll(migrateBand(el));
            }
        }
        return changes;
    }

    private List<String> migrateBand(Element band) {
        List<String> changes = new ArrayList<>();

        // isSplitAllowed (very old deprecated attribute)
        Attribute isSplitAllowed = band.getAttribute("isSplitAllowed");
        if (isSplitAllowed != null) {
            String val = isSplitAllowed.getValue();
            band.removeAttribute("isSplitAllowed");
            String splitType = "true".equalsIgnoreCase(val) ? "Stretch" : "Prevent";
            if (band.getAttribute("splitType") == null) {
                band.setAttribute("splitType", splitType);
            }
            changes.add("[band] Removed deprecated isSplitAllowed='" + val +
                    "' → splitType='" + splitType + "'");
        }

        // In JR7 the default splitType changed from Stretch → Prevent.
        // Preserve JR6 default behaviour by explicitly adding splitType="Stretch" when missing.
        if (band.getAttribute("splitType") == null) {
            // Only add if band has children (non-empty band)
            if (!band.getChildren().isEmpty()) {
                band.setAttribute("splitType", "Stretch");
                changes.add("[band] Added explicit splitType='Stretch' to preserve JR6 default behaviour");
            }
        }

        return changes;
    }
}