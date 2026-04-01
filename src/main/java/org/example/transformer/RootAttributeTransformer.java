package org.example.transformer;

import org.jdom2.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles changes to jasperReport root-level attributes between JR6 and JR7:
 *
 * - isIgnorePagination renamed to ignorePagination (boolean prefix removed)
 * - isTitleNewPage      renamed to titleNewPage
 * - isSummaryNewPage    renamed to summaryNewPage
 * - isSummaryWithPageHeaderAndFooter renamed to summaryWithPageHeaderAndFooter
 * - isFloatColumnFooter renamed to floatColumnFooter
 * - printOrder value "Vertical" is now "VERTICAL" (enum style)
 * - orientation value changes to uppercase enum
 * - whenNoDataType value renames
 * - columnDirection removed (use printOrder instead)
 */
public class RootAttributeTransformer extends AbstractJrxmlTransformer {

    @Override
    public String getName() {
        return "RootAttribute";
    }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        Element root = getRoot(document);

        // is* prefix removal (boolean attributes)
        String[][] booleanRenames = {
                {"isIgnorePagination",                    "ignorePagination"},
                {"isTitleNewPage",                         "titleNewPage"},
                {"isSummaryNewPage",                       "summaryNewPage"},
                {"isSummaryWithPageHeaderAndFooter",       "summaryWithPageHeaderAndFooter"},
                {"isFloatColumnFooter",                    "floatColumnFooter"},
        };
        for (String[] rename : booleanRenames) {
            addChange(changes, renameAttribute(root, rename[0], rename[1]));
        }

        // printOrder enum values
        rewriteEnumAttr(root, "printOrder", changes,
                new String[]{"Vertical",   "VERTICAL"},
                new String[]{"Horizontal", "HORIZONTAL"}
        );

        // orientation enum values
        rewriteEnumAttr(root, "orientation", changes,
                new String[]{"Portrait",  "PORTRAIT"},
                new String[]{"Landscape", "LANDSCAPE"}
        );

        // whenNoDataType enum values
        rewriteEnumAttr(root, "whenNoDataType", changes,
                new String[]{"NoPages",             "NO_PAGES"},
                new String[]{"BlankPage",           "BLANK_PAGE"},
                new String[]{"AllSectionsNoDetail", "ALL_SECTIONS_NO_DETAIL"},
                new String[]{"NoDataSection",       "NO_DATA_SECTION"}
        );

        // whenResourceMissingType enum
        rewriteEnumAttr(root, "whenResourceMissingType", changes,
                new String[]{"Null",  "NULL"},
                new String[]{"Empty", "EMPTY"},
                new String[]{"Key",   "KEY"},
                new String[]{"Error", "ERROR"}
        );

        return changes;
    }

    private void rewriteEnumAttr(Element el, String attrName, List<String> changes,
                                 String[]... pairs) {
        Attribute attr = el.getAttribute(attrName);
        if (attr == null) return;
        for (String[] pair : pairs) {
            String change = rewriteAttributeValue(el, attrName, pair[0], pair[1]);
            if (change != null) {
                changes.add(change);
                return;
            }
        }
    }
}