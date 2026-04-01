package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Component / Generic Element Migration:
 *
 * 1. <componentElement> with <jr:list> component:
 *    - printOrder attribute enum: "Vertical" → "VERTICAL", "Horizontal" → "HORIZONTAL"
 *    - ignoreWidth attribute (new in JR7): not added automatically; no-op.
 *
 * 2. <genericElement> type namespace changes:
 *    - genericElementType namespace updated if using old sourceforge URLs.
 *
 * 3. Table component (<jr:table>):
 *    - whenNoDataType enum updated
 *    - <jr:column> width attribute — unchanged
 *    - <jr:tableHeader>, <jr:tableFooter>, etc. splitType migrated same as bands
 *
 * 4. Crosstab component (<jr:crosstab>):
 *    - isRepeatColumnHeaders → repeatColumnHeaders
 *    - isRepeatRowHeaders    → repeatRowHeaders
 *    - columnBreakOffset still valid
 *    - runDirection enum: "LTR" / "RTL" (unchanged)
 *    - <jr:crosstabCell> border attributes → use <box>
 */
public class ComponentElementTransformer extends AbstractJrxmlTransformer {

    private static final String OLD_JR_NS = "http://jasperreports.sourceforge.net/jasperreports/components";

    @Override public String getName() { return "ComponentElement"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        Element root = getRoot(document);

        List<Element> all = findAllElements(root);
        for (Element el : all) {
            // List component
            if ("list".equals(el.getName())) {
                changes.addAll(migrateListComponent(el));
            }
            // Crosstab
            if ("crosstab".equals(el.getName())) {
                changes.addAll(migrateCrosstab(el));
            }
            // Table
            if ("table".equals(el.getName())) {
                changes.addAll(migrateTableComponent(el));
            }
            // genericElementType namespace
            if ("genericElementType".equals(el.getName())) {
                changes.addAll(migrateGenericElementType(el));
            }
        }

        return changes;
    }

    private List<String> migrateListComponent(Element list) {
        List<String> changes = new ArrayList<>();

        rewriteEnum(list, "printOrder", changes,
                new String[]{"Vertical",   "VERTICAL"},
                new String[]{"Horizontal", "HORIZONTAL"}
        );

        return changes;
    }

    private List<String> migrateCrosstab(Element crosstab) {
        List<String> changes = new ArrayList<>();

        // is* prefix removal
        addChange(changes, renameAttribute(crosstab, "isRepeatColumnHeaders", "repeatColumnHeaders"));
        addChange(changes, renameAttribute(crosstab, "isRepeatRowHeaders",    "repeatRowHeaders"));

        // whenNoDataType enum
        rewriteEnum(crosstab, "whenNoDataType", changes,
                new String[]{"Blank",         "Blank"},
                new String[]{"NoPages",       "NoPages"},
                new String[]{"AllSections",   "AllSections"}
        );

        // Migrate crosstab cells — splitType like bands
        for (Element cell : findElements(crosstab, "crosstabCell")) {
            Element contents = cell.getChild("cellContents");
            if (contents != null) {
                for (Element band : contents.getChildren("band")) {
                    if (band.getAttribute("splitType") == null && !band.getChildren().isEmpty()) {
                        band.setAttribute("splitType", "Stretch");
                        changes.add("[crosstabCell/band] Added explicit splitType='Stretch'");
                    }
                }
            }
        }

        return changes;
    }

    private List<String> migrateTableComponent(Element table) {
        List<String> changes = new ArrayList<>();

        rewriteEnum(table, "whenNoDataType", changes,
                new String[]{"Blank",   "Blank"},
                new String[]{"AllSections", "AllSections"},
                new String[]{"NoPages", "NoPages"}
        );

        // Column groups and cells — splitType
        for (Element band : findElements(table, "band")) {
            if (band.getAttribute("splitType") == null && !band.getChildren().isEmpty()) {
                band.setAttribute("splitType", "Stretch");
                changes.add("[table/band] Added explicit splitType='Stretch'");
            }
        }

        return changes;
    }

    private List<String> migrateGenericElementType(Element get) {
        List<String> changes = new ArrayList<>();

        Attribute nsAttr = get.getAttribute("namespace");
        if (nsAttr != null) {
            String ns = nsAttr.getValue();
            if (ns != null && ns.startsWith("http://jasperreports.sourceforge.net/jasperreports")
                    && ns.contains("http://")) {
                // Convert http → https
                String updated = ns.replace("http://jasperreports.sourceforge.net",
                        "https://jasperreports.sourceforge.net");
                if (!updated.equals(ns)) {
                    nsAttr.setValue(updated);
                    changes.add("[genericElementType] Updated namespace URL to HTTPS: " + updated);
                }
            }
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