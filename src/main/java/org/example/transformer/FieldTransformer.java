package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Field Migration:
 *
 * 1. <field> element:
 *    - <fieldDescription> child element is still valid in JR7.
 *    - class attribute: ensure java.lang.* short names are fully qualified.
 *      JR7 is stricter about class names in some contexts.
 *
 * 2. fieldValueClass (very old attribute) → class
 *
 * 3. Warn on java.util.Date — JR7 recommends java.time.LocalDate for new reports.
 */
public class FieldTransformer extends AbstractJrxmlTransformer {

    private static final Map<String, String> SHORT_CLASS_MAP = new LinkedHashMap<>();

    static {
        SHORT_CLASS_MAP.put("String",     "java.lang.String");
        SHORT_CLASS_MAP.put("Integer",    "java.lang.Integer");
        SHORT_CLASS_MAP.put("Long",       "java.lang.Long");
        SHORT_CLASS_MAP.put("Double",     "java.lang.Double");
        SHORT_CLASS_MAP.put("Float",      "java.lang.Float");
        SHORT_CLASS_MAP.put("Boolean",    "java.lang.Boolean");
        SHORT_CLASS_MAP.put("Short",      "java.lang.Short");
        SHORT_CLASS_MAP.put("Byte",       "java.lang.Byte");
        SHORT_CLASS_MAP.put("BigDecimal", "java.math.BigDecimal");
        SHORT_CLASS_MAP.put("BigInteger", "java.math.BigInteger");
        SHORT_CLASS_MAP.put("Date",       "java.util.Date");
        SHORT_CLASS_MAP.put("Timestamp",  "java.sql.Timestamp");
        SHORT_CLASS_MAP.put("Time",       "java.sql.Time");
        SHORT_CLASS_MAP.put("Object",     "java.lang.Object");
        SHORT_CLASS_MAP.put("Collection", "java.util.Collection");
    }

    @Override public String getName() { return "Field"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("field".equals(el.getName())) {
                changes.addAll(migrateField(el));
            }
            // Same class attribute exists on variable elements
            if ("variable".equals(el.getName())) {
                changes.addAll(migrateClassAttr(el));
            }
            if ("parameter".equals(el.getName())) {
                changes.addAll(migrateClassAttr(el));
            }
        }
        return changes;
    }

    private List<String> migrateField(Element field) {
        List<String> changes = new ArrayList<>();

        // fieldValueClass (very old) → class
        addChange(changes, renameAttribute(field, "fieldValueClass", "class"));

        changes.addAll(migrateClassAttr(field));
        return changes;
    }

    private List<String> migrateClassAttr(Element el) {
        List<String> changes = new ArrayList<>();
        Attribute classAttr = el.getAttribute("class");
        if (classAttr == null) return changes;

        String val = classAttr.getValue().trim();

        // Expand short names to fully qualified
        if (SHORT_CLASS_MAP.containsKey(val)) {
            String fqn = SHORT_CLASS_MAP.get(val);
            classAttr.setValue(fqn);
            changes.add(String.format("[%s name='%s'] Expanded class='%s' → '%s'",
                    el.getName(), el.getAttributeValue("name"), val, fqn));
        }

        return changes;
    }
}
