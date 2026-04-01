package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Export Configuration Migration:
 *
 * JasperReports 7 replaced the per-element export configuration attributes with
 * a unified <exportConfiguration> element approach, and many old per-element
 * export hints have been either renamed or removed.
 *
 * Key changes handled here:
 *
 * 1. <printWhenExpression> — no change needed, still valid.
 *
 * 2. Per-element XLS export hints that were attributes in JR6:
 *    - <element> child <jr:xls> deprecated in favour of property-based config.
 *    - Convert any remaining <jr:xls> / <jr:html> inline export config elements
 *      to <property> elements with the correct keys.
 *
 * 3. <jasperReport> level export configurations:
 *    - net.sf.jasperreports.export.pdf.pdfa.conformance values:
 *      "pdfa1a" → "PDFA1A", "pdfa1b" → "PDFA1B", "pdfa2a" → "PDFA2A"
 *
 * 4. Ignored elements — log a warning but do not remove:
 *    - <jr:exportConfiguration> (JR6 API, deprecated in JR7)
 */
public class ExportConfigTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "ExportConfig"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        List<Element> all = findAllElements(getRoot(document));

        for (Element el : all) {
            // Migrate inline XLS export configurations
            if ("xls".equals(el.getName()) || "xlsx".equals(el.getName())) {
                changes.addAll(migrateInlineExportConfig(el, "xls"));
            }
            if ("html".equals(el.getName()) && el.getParentElement() != null
                    && "reportElement".equals(el.getParentElement().getName())) {
                changes.addAll(migrateInlineExportConfig(el, "html"));
            }

            // PDF conformance value normalization on property elements
            if ("property".equals(el.getName())) {
                String name = el.getAttributeValue("name");
                if ("net.sf.jasperreports.export.pdf.pdfa.conformance".equals(name)) {
                    Attribute val = el.getAttribute("value");
                    if (val != null) {
                        String normalized = normalizePdfaConformance(val.getValue());
                        if (!normalized.equals(val.getValue())) {
                            val.setValue(normalized);
                            changes.add("[property pdfa.conformance] Normalized value '"
                                    + val.getValue() + "' → '" + normalized + "'");
                        }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Convert inline <jr:xls collapseRowSpan="true" ...> export hint elements
     * to <property> elements on the parent reportElement.
     */
    private List<String> migrateInlineExportConfig(Element exportEl, String exportType) {
        List<String> changes = new ArrayList<>();
        Element parent = exportEl.getParentElement();
        if (parent == null) return changes;

        List<Attribute> attrs = new ArrayList<>(exportEl.getAttributes());
        for (Attribute attr : attrs) {
            String propKey = buildPropertyKey(exportType, attr.getName());
            if (propKey != null) {
                Element prop = new Element("property");
                prop.setAttribute("name", propKey);
                prop.setAttribute("value", attr.getValue());
                parent.addContent(prop);
                changes.add(String.format(
                        "[%s] Converted inline export hint %s.%s='%s' → <property name='%s' value='%s'/>",
                        parent.getName(), exportType, attr.getName(), attr.getValue(), propKey, attr.getValue()));
            }
        }

        if (!attrs.isEmpty()) {
            exportEl.detach();
            changes.add(String.format("[%s] Removed inline <%s> export configuration element (converted to properties)",
                    parent.getName(), exportType));
        }

        return changes;
    }

    private String buildPropertyKey(String type, String attrName) {
        Map<String, String> xlsMapping = new LinkedHashMap<>();
        xlsMapping.put("collapseRowSpan",     "net.sf.jasperreports.export.xls.collapse.row.span");
        xlsMapping.put("breakBeforeRow",      "net.sf.jasperreports.export.xls.break.before.row");
        xlsMapping.put("breakAfterRow",       "net.sf.jasperreports.export.xls.break.after.row");
        xlsMapping.put("sheetName",           "net.sf.jasperreports.export.xls.sheet.name");
        xlsMapping.put("startPageIndex",      "net.sf.jasperreports.export.xls.start.page.index");
        xlsMapping.put("endPageIndex",        "net.sf.jasperreports.export.xls.end.page.index");
        xlsMapping.put("printNoLineBreaks",   "net.sf.jasperreports.export.xls.print.no.line.breaks");

        Map<String, String> htmlMapping = new LinkedHashMap<>();
        htmlMapping.put("isRemoveLineBreaks", "net.sf.jasperreports.export.html.remove.line.breaks");
        htmlMapping.put("isWhiteBackground",  "net.sf.jasperreports.export.html.white.background");

        if ("xls".equals(type) || "xlsx".equals(type)) return xlsMapping.get(attrName);
        if ("html".equals(type)) return htmlMapping.get(attrName);
        return null;
    }

    private String normalizePdfaConformance(String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("pdfa1a", "PDFA1A");
        map.put("pdfa1b", "PDFA1B");
        map.put("pdfa2a", "PDFA2A");
        map.put("pdfa2b", "PDFA2B");
        map.put("pdfa3a", "PDFA3A");
        map.put("pdfa3b", "PDFA3B");
        return map.getOrDefault(value.toLowerCase(), value);
    }
}
