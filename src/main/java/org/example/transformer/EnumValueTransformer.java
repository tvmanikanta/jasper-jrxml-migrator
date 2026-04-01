package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Global Enum Value Migration:
 *
 * Handles any remaining enum-style attribute values across all elements
 * that follow the JR6 → JR7 TitleCase-to-UPPER_CASE convention change,
 * or other specific value renames.
 *
 * Covers:
 * - positionType:    "Float" → "FLOAT", "FixRelativeToTop" → "FIX_RELATIVE_TO_TOP",
 *                    "FixRelativeToBottom" → "FIX_RELATIVE_TO_BOTTOM"
 * - stretchType:     "NoStretch" → "NO_STRETCH", "RelativeToTallestObject" → "RELATIVE_TO_TALLEST_OBJECT",
 *                    "RelativeToBandHeight" → "RELATIVE_TO_BAND_HEIGHT",
 *                    "RelativeToReportElement" → "RELATIVE_TO_REPORT_ELEMENT",
 *                    "ElementGroupHeight" → "ELEMENT_GROUP_HEIGHT",
 *                    "ElementGroupTop" → "ELEMENT_GROUP_TOP",
 *                    "ContainerHeight" → "CONTAINER_HEIGHT",
 *                    "ContainerBottom" → "CONTAINER_BOTTOM"
 * - pen lineStyle:   "Solid", "Dashed", "Dotted", "Double" (already title-case — unchanged in JR7)
 * - runDirection:    "LTR" / "RTL" (unchanged)
 * - sectionType:     "Band" (unchanged)
 * - fillValue:       "Solid" / "None" (unchanged)
 */
public class EnumValueTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "EnumValue"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        List<Element> all = findAllElements(getRoot(document));

        for (Element el : all) {
            // positionType
            rewriteEnum(el, "positionType", changes,
                    new String[]{"Float",                 "FLOAT"},
                    new String[]{"FixRelativeToTop",      "FIX_RELATIVE_TO_TOP"},
                    new String[]{"FixRelativeToBottom",   "FIX_RELATIVE_TO_BOTTOM"}
            );

            // stretchType
            rewriteEnum(el, "stretchType", changes,
                    new String[]{"NoStretch",                    "NO_STRETCH"},
                    new String[]{"RelativeToTallestObject",      "RELATIVE_TO_TALLEST_OBJECT"},
                    new String[]{"RelativeToBandHeight",         "RELATIVE_TO_BAND_HEIGHT"},
                    new String[]{"RelativeToReportElement",      "RELATIVE_TO_REPORT_ELEMENT"},
                    new String[]{"ElementGroupHeight",           "ELEMENT_GROUP_HEIGHT"},
                    new String[]{"ElementGroupTop",              "ELEMENT_GROUP_TOP"},
                    new String[]{"ContainerHeight",              "CONTAINER_HEIGHT"},
                    new String[]{"ContainerBottom",              "CONTAINER_BOTTOM"}
            );

            // sectionType (on section elements)
            rewriteEnum(el, "sectionType", changes,
                    new String[]{"Band", "Band"} // unchanged but normalize if old lowercase
            );

            // evaluationTime on all element types
            rewriteEnum(el, "evaluationTime", changes,
                    new String[]{"Now",    "Now"},
                    new String[]{"Report", "Report"},
                    new String[]{"Page",   "Page"},
                    new String[]{"Column", "Column"},
                    new String[]{"Group",  "Group"},
                    new String[]{"Band",   "Band"},
                    new String[]{"Auto",   "Auto"}
            );

            // calculation on variable
            rewriteEnum(el, "calculation", changes,
                    new String[]{"Nothing",           "Nothing"},
                    new String[]{"Count",             "Count"},
                    new String[]{"DistinctCount",     "DistinctCount"},
                    new String[]{"Sum",               "Sum"},
                    new String[]{"Average",           "Average"},
                    new String[]{"Lowest",            "Lowest"},
                    new String[]{"Highest",           "Highest"},
                    new String[]{"StandardDeviation", "StandardDeviation"},
                    new String[]{"Variance",          "Variance"},
                    new String[]{"First",             "First"},
                    new String[]{"System",            "System"}
            );

            // resetType / incrementType
            rewriteEnum(el, "resetType", changes,
                    new String[]{"Report", "Report"}, new String[]{"Page",   "Page"},
                    new String[]{"Column", "Column"}, new String[]{"Group",  "Group"},
                    new String[]{"None",   "None"},   new String[]{"Master", "Master"}
            );
            rewriteEnum(el, "incrementType", changes,
                    new String[]{"Report", "Report"}, new String[]{"Page",   "Page"},
                    new String[]{"Column", "Column"}, new String[]{"Group",  "Group"},
                    new String[]{"None",   "None"}
            );
        }

        return changes;
    }

    private void rewriteEnum(Element el, String attr, List<String> changes, String[]... pairs) {
        for (String[] p : pairs) {
            String c = rewriteAttributeValue(el, attr, p[0], p[1]);
            if (c != null) { changes.add(c); return; }
        }
    }
}