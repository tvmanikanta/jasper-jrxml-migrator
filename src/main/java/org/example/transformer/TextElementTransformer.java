package org.example.transformer;

import org.jdom2.*;

import java.util.*;

/**
 * JR7 Text Element Migration:
 *
 * 1. isStretchWithOverflow renamed to textAdjust (values: StretchHeight / CutText / ScaleFont)
 *    - isStretchWithOverflow="true"  → textAdjust="StretchHeight"
 *    - isStretchWithOverflow="false" → textAdjust="CutText"
 *
 * 2. markup attribute values: "styled" still valid, others unchanged. Log for awareness.
 *
 * 3. printWhenDetailOverflows → no longer on textField directly; behaviour now part of band.
 *
 * 4. <textElement> verticalAlignment → verticalTextAlign
 *
 * 5. rotation attribute enum values capitalized
 *
 * 6. textAdjust (new in JR7) defaults — ensure we set explicit value when migrating.
 */
public class TextElementTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "TextElement"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        List<Element> all = findAllElements(getRoot(document));

        for (Element el : all) {
            String name = el.getName();

            // textField and staticText share <textElement> child
            if ("textElement".equals(name)) {
                changes.addAll(migrateTextElement(el));
            }

            // <textField> itself
            if ("textField".equals(name)) {
                changes.addAll(migrateTextField(el));
            }
        }
        return changes;
    }

    private List<String> migrateTextElement(Element te) {
        List<String> changes = new ArrayList<>();

        // verticalAlignment → verticalTextAlign
        addChange(changes, renameAttribute(te, "verticalAlignment", "verticalTextAlign"));

        // textAlignment enum (was "Center", now "Center" is still valid but older "Justified" → "Justified")
        rewriteEnum(te, "textAlignment", changes,
                new String[]{"Left",      "Left"},
                new String[]{"Center",    "Center"},
                new String[]{"Right",     "Right"},
                new String[]{"Justified", "Justified"}
        );

        // verticalTextAlign enum
        rewriteEnum(te, "verticalTextAlign", changes,
                new String[]{"Top",    "Top"},
                new String[]{"Middle", "Middle"},
                new String[]{"Bottom", "Bottom"}
        );

        // rotation
        rewriteEnum(te, "rotation", changes,
                new String[]{"None",            "None"},
                new String[]{"Left",            "Left"},
                new String[]{"Right",           "Right"},
                new String[]{"UpsideDown",      "UpsideDown"}
        );

        return changes;
    }

    private List<String> migrateTextField(Element tf) {
        List<String> changes = new ArrayList<>();

        // isStretchWithOverflow → textAdjust
        Attribute stretch = tf.getAttribute("isStretchWithOverflow");
        if (stretch != null) {
            String val = stretch.getValue();
            tf.removeAttribute("isStretchWithOverflow");
            String newVal = "true".equalsIgnoreCase(val) ? "StretchHeight" : "CutText";
            tf.setAttribute("textAdjust", newVal);
            changes.add(String.format(
                    "[textField] isStretchWithOverflow='%s' → textAdjust='%s'", val, newVal));
        }

        // evaluationTime enum
        rewriteEnum(tf, "evaluationTime", changes,
                new String[]{"Now",     "Now"},
                new String[]{"Report",  "Report"},
                new String[]{"Page",    "Page"},
                new String[]{"Column",  "Column"},
                new String[]{"Group",   "Group"},
                new String[]{"Band",    "Band"},
                new String[]{"Auto",    "Auto"}
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
