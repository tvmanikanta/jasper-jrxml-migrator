package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Style Migration:
 *
 * 1. <style> element:
 *    - hAlign → textAlignment (on text styles)
 *    - vAlign → verticalTextAlign (on text styles)
 *    - hTextAlign → textAlignment
 *    - vTextAlign → verticalTextAlign
 *    - border/topBorder/etc. attributes — moved to <box> element; warn if found.
 *    - radius attribute still supported.
 *
 * 2. <conditionalStyle> element: condition child is now conditionExpression (renamed).
 *
 * 3. fill attribute: "Solid" still valid, "None" still valid.
 *
 * 4. scaleImage on styles → still valid.
 *
 * 5. isBlankWhenNull on style → removed from style; must be on element itself.
 */
public class StyleTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "Style"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("style".equals(el.getName())) {
                changes.addAll(migrateStyle(el));
            }
            if ("conditionalStyle".equals(el.getName())) {
                changes.addAll(migrateConditionalStyle(el));
            }
        }
        return changes;
    }

    private List<String> migrateStyle(Element style) {
        List<String> changes = new ArrayList<>();

        // hAlign → textAlignment
        addChange(changes, renameAttribute(style, "hAlign", "textAlignment"));
        addChange(changes, renameAttribute(style, "hTextAlign", "textAlignment"));

        // vAlign → verticalTextAlign
        addChange(changes, renameAttribute(style, "vAlign", "verticalTextAlign"));
        addChange(changes, renameAttribute(style, "vTextAlign", "verticalTextAlign"));

        // isBlankWhenNull removed from style level
        Attribute blankWhenNull = style.getAttribute("isBlankWhenNull");
        if (blankWhenNull != null) {
            style.removeAttribute("isBlankWhenNull");
            changes.add(String.format(
                    "[style name='%s'] Removed isBlankWhenNull from style (must be set on individual elements in JR7)",
                    style.getAttributeValue("name")));
        }

        // Warn about border shorthand attributes (pen/border moved to <box> in JR5+; still possible in legacy)
        for (String deprecated : Arrays.asList("border", "borderColor",
                "topBorder", "topBorderColor", "topPadding",
                "leftBorder", "leftBorderColor", "leftPadding",
                "rightBorder", "rightBorderColor", "rightPadding",
                "bottomBorder", "bottomBorderColor", "bottomPadding")) {
            Attribute attr = style.getAttribute(deprecated);
            if (attr != null) {
                style.removeAttribute(deprecated);
                changes.add(String.format(
                        "[style] Removed legacy border attribute '%s' (use <box> child element in JR7)", deprecated));
            }
        }

        return changes;
    }

    private List<String> migrateConditionalStyle(Element cs) {
        List<String> changes = new ArrayList<>();

        // conditionExpression still correct name; no change needed.
        // In some very old JR5 files it was called "condition" without "Expression"
        Element condition = cs.getChild("condition");
        if (condition != null && cs.getChild("conditionExpression") == null) {
            condition.setName("conditionExpression");
            changes.add("[conditionalStyle] Renamed <condition> → <conditionExpression>");
        }

        return changes;
    }
}
