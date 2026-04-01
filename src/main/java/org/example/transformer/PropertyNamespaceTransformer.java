package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Property Namespace Migration:
 *
 * Many net.sf.jasperreports.* property names were renamed or relocated in JR7.
 * This transformer scans all <property> and <propertyExpression> elements and
 * applies the known rename map.
 *
 * Key renamed properties (JR6 → JR7):
 *
 * Export-related:
 *   net.sf.jasperreports.export.pdf.font.*                 (unchanged)
 *   net.sf.jasperreports.export.pdf.pdfa.conformance        → net.sf.jasperreports.export.pdf.pdfa.conformance (unchanged)
 *   net.sf.jasperreports.export.xls.sheet.names.*           (unchanged)
 *   net.sf.jasperreports.export.xls.freeze.row.edge        → net.sf.jasperreports.export.xls.freeze.row
 *   net.sf.jasperreports.export.xls.freeze.column.edge     → net.sf.jasperreports.export.xls.freeze.column
 *
 * Print/render:
 *   net.sf.jasperreports.print.keep.full.snapshot          → removed (use JR7 virtualizer API)
 *
 * HTML Exporter:
 *   net.sf.jasperreports.export.html.id                    → net.sf.jasperreports.export.html.id (unchanged)
 *   net.sf.jasperreports.html.class                        → net.sf.jasperreports.export.html.class
 *
 * Component properties:
 *   net.sf.jasperreports.components.list.*                  (unchanged)
 *
 * Scriptlets:
 *   net.sf.jasperreports.scriptlet.class                   (unchanged)
 */
public class PropertyNamespaceTransformer extends AbstractJrxmlTransformer {

    private static final Map<String, String> PROPERTY_RENAMES = new LinkedHashMap<>();

    static {
        // XLS freeze row/column edge
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.export.xls.freeze.row.edge",
                "net.sf.jasperreports.export.xls.freeze.row");
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.export.xls.freeze.column.edge",
                "net.sf.jasperreports.export.xls.freeze.column");

        // HTML class property (old shorthand)
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.html.class",
                "net.sf.jasperreports.export.html.class");
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.html.id",
                "net.sf.jasperreports.export.html.id");

        // Chart theme property removed
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.chart.theme",
                null); // null = remove

        // PDF version property rename
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.export.pdf.version",
                "net.sf.jasperreports.export.pdf.version"); // unchanged, just ensure it exists

        // Virtualizer snapshot — deprecated
        PROPERTY_RENAMES.put(
                "net.sf.jasperreports.print.keep.full.snapshot",
                null); // remove — virtualizer API changed completely
    }

    @Override public String getName() { return "PropertyNamespace"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        List<Element> all = findAllElements(getRoot(document));

        for (Element el : all) {
            if ("property".equals(el.getName())) {
                changes.addAll(migrateProperty(el));
            } else if ("propertyExpression".equals(el.getName())) {
                changes.addAll(migratePropertyExpression(el));
            }
        }
        return changes;
    }

    private List<String> migrateProperty(Element prop) {
        List<String> changes = new ArrayList<>();
        String name = prop.getAttributeValue("name");
        if (name == null) return changes;

        if (PROPERTY_RENAMES.containsKey(name)) {
            String newName = PROPERTY_RENAMES.get(name);
            if (newName == null) {
                // Remove deprecated property
                String value = prop.getAttributeValue("value");
                prop.detach();
                changes.add(String.format(
                        "[property] Removed deprecated property name='%s' value='%s'", name, value));
            } else if (!newName.equals(name)) {
                prop.setAttribute("name", newName);
                changes.add(String.format(
                        "[property] Renamed property '%s' → '%s'", name, newName));
            }
        }
        return changes;
    }

    private List<String> migratePropertyExpression(Element propExpr) {
        List<String> changes = new ArrayList<>();
        String name = propExpr.getAttributeValue("name");
        if (name == null) return changes;

        if (PROPERTY_RENAMES.containsKey(name)) {
            String newName = PROPERTY_RENAMES.get(name);
            if (newName == null) {
                propExpr.detach();
                changes.add(String.format(
                        "[propertyExpression] Removed deprecated property name='%s'", name));
            } else if (!newName.equals(name)) {
                propExpr.setAttribute("name", newName);
                changes.add(String.format(
                        "[propertyExpression] Renamed property '%s' → '%s'", name, newName));
            }
        }
        return changes;
    }
}
