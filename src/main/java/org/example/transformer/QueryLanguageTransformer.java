package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * Query language attribute canonicalization.
 * JR7 expects lowercase language identifiers.
 */
public class QueryLanguageTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "QueryLanguage"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("queryString".equals(el.getName())) {
                Attribute lang = el.getAttribute("language");
                if (lang != null) {
                    String val = lang.getValue();
                    String lower = val.toLowerCase();
                    if (!val.equals(lower)) {
                        lang.setValue(lower);
                        changes.add(String.format(
                                "[queryString] Normalized language='%s' → '%s'", val, lower));
                    }
                }
            }
        }
        return changes;
    }
}
