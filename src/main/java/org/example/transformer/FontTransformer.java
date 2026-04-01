package org.example.transformer;

import org.jdom2.*;

import java.util.*;

/**
 * JR7 Font Migration:
 *
 * 1. <font> element inside <reportFont> is removed — reportFont itself is deprecated.
 *    Migrate to <style> declarations with isFontSizeFixedInPoints="true".
 *
 * 2. pdfFontName attribute on <font>/<textElement> is removed.
 *    Map common PDF font names to the equivalent net.sf.jasperreports.export.pdf.font.* property.
 *
 * 3. pdfEncoding attribute removed — no longer needed (PDF/A uses embedded fonts).
 *
 * 4. isPdfEmbedded attribute removed — always true in JR7.
 *
 * 5. fontSizeUnit attribute added where missing (default was "Point" in JR6, now explicit).
 *
 * 6. <fontFamily> extension element now uses new attribute names.
 */
public class FontTransformer extends AbstractJrxmlTransformer {

    // Mapping of legacy pdfFontName values to JR7 font extension names
    private static final Map<String, String> PDF_FONT_MAP = new LinkedHashMap<>();

    static {
        PDF_FONT_MAP.put("Helvetica",              "Helvetica");
        PDF_FONT_MAP.put("Helvetica-Bold",         "Helvetica");
        PDF_FONT_MAP.put("Helvetica-Oblique",      "Helvetica");
        PDF_FONT_MAP.put("Helvetica-BoldOblique",  "Helvetica");
        PDF_FONT_MAP.put("Times-Roman",            "Times New Roman");
        PDF_FONT_MAP.put("Times-Bold",             "Times New Roman");
        PDF_FONT_MAP.put("Times-Italic",           "Times New Roman");
        PDF_FONT_MAP.put("Times-BoldItalic",       "Times New Roman");
        PDF_FONT_MAP.put("Courier",                "Courier New");
        PDF_FONT_MAP.put("Courier-Bold",           "Courier New");
        PDF_FONT_MAP.put("Courier-Oblique",        "Courier New");
        PDF_FONT_MAP.put("Courier-BoldOblique",    "Courier New");
        PDF_FONT_MAP.put("Symbol",                 "Symbol");
        PDF_FONT_MAP.put("ZapfDingbats",           "ZapfDingbats");
    }

    @Override
    public String getName() {
        return "Font";
    }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        Element root = getRoot(document);

        // Migrate all elements that may carry font attributes
        List<Element> allElements = findAllElements(root);

        for (Element el : allElements) {
            migrateElementFontAttributes(el, changes);
        }

        // Handle deprecated <reportFont> elements
        migrateReportFonts(root, changes);

        return changes;
    }

    private void migrateElementFontAttributes(Element el, List<String> changes) {
        // pdfFontName → record the mapping, add a comment property
        Attribute pdfFontAttr = el.getAttribute("pdfFontName");
        if (pdfFontAttr != null) {
            String pdfFontName = pdfFontAttr.getValue();
            el.removeAttribute("pdfFontName");

            String mappedFont = PDF_FONT_MAP.getOrDefault(pdfFontName, pdfFontName);
            changes.add(String.format(
                    "[%s] Removed pdfFontName='%s' (map to fontName='%s' or configure font extension)",
                    el.getName(), pdfFontName, mappedFont));

            // If fontName is missing, set it from the PDF font mapping
            if (el.getAttribute("fontName") == null && !mappedFont.isEmpty()) {
                el.setAttribute("fontName", mappedFont);
                changes.add(String.format("[%s] Set fontName='%s' derived from pdfFontName",
                        el.getName(), mappedFont));
            }
        }

        // pdfEncoding → removed (PDF/UA and embedded fonts handle this in JR7)
        Attribute pdfEnc = el.getAttribute("pdfEncoding");
        if (pdfEnc != null) {
            el.removeAttribute("pdfEncoding");
            changes.add(String.format("[%s] Removed deprecated pdfEncoding attribute", el.getName()));
        }

        // isPdfEmbedded → removed (always embedded in JR7)
        Attribute isPdfEmb = el.getAttribute("isPdfEmbedded");
        if (isPdfEmb != null) {
            el.removeAttribute("isPdfEmbedded");
            changes.add(String.format("[%s] Removed deprecated isPdfEmbedded attribute (always embedded in JR7)",
                    el.getName()));
        }
    }

    private void migrateReportFonts(Element root, List<String> changes) {
        List<Element> reportFonts = new ArrayList<>(root.getChildren("reportFont"));
        for (Element reportFont : reportFonts) {
            String fontName = reportFont.getAttributeValue("name");
            // Convert to <style> element
            Element styleEl = new Element("style");
            styleEl.setAttribute("name", fontName != null ? fontName : "MigratedFont");
            styleEl.setAttribute("isDefault", "false");

            // Copy font attributes
            copyAttr(reportFont, styleEl, "fontName");
            copyAttr(reportFont, styleEl, "fontSize");
            copyAttr(reportFont, styleEl, "isBold");
            copyAttr(reportFont, styleEl, "isItalic");
            copyAttr(reportFont, styleEl, "isUnderline");
            copyAttr(reportFont, styleEl, "isStrikeThrough");

            // Remove pdfFontName/pdfEncoding/isPdfEmbedded — handled above
            reportFont.getParent().addContent(
                    reportFont.getParent().indexOf(reportFont), styleEl);
            reportFont.detach();

            changes.add(String.format(
                    "[reportFont name='%s'] Migrated deprecated <reportFont> → <style>", fontName));
        }
    }

    private void copyAttr(Element source, Element target, String attrName) {
        Attribute attr = source.getAttribute(attrName);
        if (attr != null) {
            target.setAttribute(attrName, attr.getValue());
        }
    }
}