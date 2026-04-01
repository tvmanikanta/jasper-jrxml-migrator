package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Subreport Migration:
 *
 * 1. isUsingCache — deprecated on subreport, remove it.
 *
 * 2. <returnValue> element changes:
 *    - toVariable renamed to variable
 *    - calculation attribute enum values (same as variable calculation — normalize)
 *
 * 3. <subreportParameter> — no changes needed structurally; validate expression wrappers.
 */
public class SubreportTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "Subreport"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("subreport".equals(el.getName())) {
                changes.addAll(migrateSubreport(el));
            }
        }
        return changes;
    }

    private List<String> migrateSubreport(Element sr) {
        List<String> changes = new ArrayList<>();

        addChange(changes, removeAttribute(sr, "isUsingCache",
                "caching deprecated in JR7 subreports"));

        // returnValue: toVariable → variable
        for (Element rv : sr.getChildren("returnValue")) {
            addChange(changes, renameAttribute(rv, "toVariable", "variable"));

            // calculation enum (capitalize if old-style)
            rewriteEnum(rv, "calculation", changes,
                    new String[]{"Nothing",          "Nothing"},
                    new String[]{"Count",            "Count"},
                    new String[]{"DistinctCount",    "DistinctCount"},
                    new String[]{"Sum",              "Sum"},
                    new String[]{"Average",          "Average"},
                    new String[]{"Lowest",           "Lowest"},
                    new String[]{"Highest",          "Highest"},
                    new String[]{"StandardDeviation","StandardDeviation"},
                    new String[]{"Variance",         "Variance"},
                    new String[]{"First",            "First"},
                    new String[]{"System",           "System"}
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