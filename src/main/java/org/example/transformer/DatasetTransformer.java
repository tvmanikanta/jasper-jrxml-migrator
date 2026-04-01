package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Dataset / Group Migration:
 *
 * 1. <group> element:
 *    - isStartNewColumn → startNewColumn
 *    - isStartNewPage   → startNewPage
 *    - isResetPageNumber → resetPageNumber
 *    - isReprintHeaderOnEachPage → reprintHeaderOnEachPage
 *    - isKeepTogether → keepTogether (added in JR6, still valid)
 *    - minHeightToStartNewPage: still valid attribute
 *
 * 2. <dataset> / <subdataset>:
 *    - whenResourceMissingType enum values
 *
 * 3. <variable> element:
 *    - calculation enum values — no case change needed (JR kept camelCase)
 *    - resetType enum: "Report", "Page", "Column", "Group", "None" — kept as-is
 *    - incrementType enum: same
 */
public class DatasetTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "Dataset"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("group".equals(el.getName())) {
                changes.addAll(migrateGroup(el));
            }
            if ("subdataset".equals(el.getName())) {
                changes.addAll(migrateSubdataset(el));
            }
        }
        return changes;
    }

    private List<String> migrateGroup(Element group) {
        List<String> changes = new ArrayList<>();

        String[][] renames = {
                {"isStartNewColumn",           "startNewColumn"},
                {"isStartNewPage",             "startNewPage"},
                {"isResetPageNumber",          "resetPageNumber"},
                {"isReprintHeaderOnEachPage",  "reprintHeaderOnEachPage"},
        };
        for (String[] r : renames) {
            addChange(changes, renameAttribute(group, r[0], r[1]));
        }

        return changes;
    }

    private List<String> migrateSubdataset(Element ds) {
        List<String> changes = new ArrayList<>();

        rewriteEnum(ds, "whenResourceMissingType", changes,
                new String[]{"Null",  "NULL"},
                new String[]{"Empty", "EMPTY"},
                new String[]{"Key",   "KEY"},
                new String[]{"Error", "ERROR"}
        );

        return changes;
    }

    private void rewriteEnum(Element el, String attr, List<String> changes, String[]... pairs) {
        for (String[] p : pairs) {
            String c = rewriteAttributeValue(el, attr, p[0], p[1]);
            if (c != null) { changes.add(c); return; }
        }
    }
}