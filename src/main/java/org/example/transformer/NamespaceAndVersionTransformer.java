package org.example.transformer;

import org.jdom2.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the jasperReport root element:
 * - xsi:schemaLocation from 6.x to 7.x URL
 * - version attribute value
 * - xmlns declarations if needed
 */
public class NamespaceAndVersionTransformer extends AbstractJrxmlTransformer {

    private static final String OLD_SCHEMA_LOC_PATTERN = "https?://jasperreports\\.sourceforge\\.net/xsd/jasperreport\\.xsd";
    private static final String NEW_SCHEMA_LOC          = "https://jasperreports.sourceforge.net/xsd/jasperreport.xsd";

    private static final String OLD_VERSION_PREFIX = "6.";
    private static final String NEW_VERSION        = "7.0.0";

    @Override
    public String getName() {
        return "NamespaceAndVersion";
    }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        Element root = getRoot(document);

        // --- schemaLocation ---
        Namespace xsiNs = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Attribute schemaLoc = root.getAttribute("schemaLocation", xsiNs);
        if (schemaLoc != null) {
            String loc = schemaLoc.getValue();
            String updated = loc.replaceAll(OLD_SCHEMA_LOC_PATTERN, NEW_SCHEMA_LOC)
                    .replace("http://jasperreports.sourceforge.net/xsd/jasperreport.xsd",
                            NEW_SCHEMA_LOC);
            if (!updated.equals(loc)) {
                schemaLoc.setValue(updated);
                changes.add("[jasperReport] Updated xsi:schemaLocation to JasperReports 7 URL");
            }
        }

        // --- version attribute ---
        Attribute versionAttr = root.getAttribute("version");
        if (versionAttr != null) {
            String v = versionAttr.getValue();
            if (v.startsWith(OLD_VERSION_PREFIX) || v.startsWith("5.") || v.startsWith("4.")) {
                versionAttr.setValue(NEW_VERSION);
                changes.add("[jasperReport] Updated version from '" + v + "' → '" + NEW_VERSION + "'");
            }
        } else {
            root.setAttribute("version", NEW_VERSION);
            changes.add("[jasperReport] Added version='" + NEW_VERSION + "' attribute");
        }

        return changes;
    }
}