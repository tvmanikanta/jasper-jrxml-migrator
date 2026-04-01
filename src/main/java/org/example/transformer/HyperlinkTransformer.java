package org.example.transformer;

import org.jdom2.*;

import java.util.*;

/**
 * Migrates <hyperlink> and anchor elements:
 * - hyperlinkTarget attribute values: "Self" → "SELF", "Blank" → "BLANK", etc.
 * - hyperlinkType attribute value canonicalization
 */
public class HyperlinkTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "Hyperlink"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        List<Element> all = findAllElements(getRoot(document));

        for (Element el : all) {
            // hyperlinkTarget enum
            rewriteEnum(el, "hyperlinkTarget", changes,
                    new String[]{"Self",          "SELF"},
                    new String[]{"Blank",         "BLANK"},
                    new String[]{"Top",           "TOP"},
                    new String[]{"Parent",        "PARENT"},
                    new String[]{"CustomSelf",    "CUSTOM_SELF"},
                    new String[]{"CustomBlank",   "CUSTOM_BLANK"},
                    new String[]{"CustomTop",     "CUSTOM_TOP"},
                    new String[]{"CustomParent",  "CUSTOM_PARENT"}
            );

            // hyperlinkType enum
            rewriteEnum(el, "hyperlinkType", changes,
                    new String[]{"None",            "NONE"},
                    new String[]{"Reference",       "REFERENCE"},
                    new String[]{"LocalAnchor",     "LOCAL_ANCHOR"},
                    new String[]{"LocalPage",       "LOCAL_PAGE"},
                    new String[]{"RemoteAnchor",    "REMOTE_ANCHOR"},
                    new String[]{"RemotePage",      "REMOTE_PAGE"},
                    new String[]{"ReportExecution", "REPORT_EXECUTION"},
                    new String[]{"Custom",          "CUSTOM"}
            );
        }
        return changes;
    }

    private void rewriteEnum(Element el, String attr, List<String> changes, String[]... pairs) {
        for (String[] pair : pairs) {
            String c = rewriteAttributeValue(el, attr, pair[0], pair[1]);
            if (c != null) { changes.add(c); return; }
        }
    }
}