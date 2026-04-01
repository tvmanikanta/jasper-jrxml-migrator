package org.example.transformer;

import org.jdom2.*;
import java.util.*;

/**
 * JR7 Image Element Migration:
 *
 * 1. onErrorType enum values: "Error" → "ERROR", "Blank" → "BLANK", "Icon" → "ICON"
 *
 * 2. isUsingCache deprecated — remove from image elements.
 *    JR7 always uses a cache; the attribute is ignored.
 *
 * 3. scaleImage attribute enum: "Clip" → "CLIP", "FillFrame" → "FILL_FRAME",
 *    "RetainShape" → "RETAIN_SHAPE", "RealHeight" → "REAL_HEIGHT",
 *    "RealSize" → "REAL_SIZE"
 *
 * 4. hAlign / vAlign → horizontalImageAlign / verticalImageAlign
 *
 * 5. isLazy attribute: still supported in JR7 but behaviour changed for remote URLs.
 */
public class ImageElementTransformer extends AbstractJrxmlTransformer {

    @Override public String getName() { return "ImageElement"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();

        for (Element el : findAllElements(getRoot(document))) {
            if ("image".equals(el.getName())) {
                changes.addAll(migrateImage(el));
            }
        }
        return changes;
    }

    private List<String> migrateImage(Element img) {
        List<String> changes = new ArrayList<>();

        // isUsingCache → removed
        addChange(changes, removeAttribute(img, "isUsingCache",
                "caching always enabled in JR7"));

        // onErrorType enum
        rewriteEnum(img, "onErrorType", changes,
                new String[]{"Error", "ERROR"},
                new String[]{"Blank", "BLANK"},
                new String[]{"Icon",  "ICON"}
        );

        // scaleImage enum
        rewriteEnum(img, "scaleImage", changes,
                new String[]{"Clip",         "CLIP"},
                new String[]{"FillFrame",    "FILL_FRAME"},
                new String[]{"RetainShape",  "RETAIN_SHAPE"},
                new String[]{"RealHeight",   "REAL_HEIGHT"},
                new String[]{"RealSize",     "REAL_SIZE"}
        );

        // hAlign → horizontalImageAlign
        addChange(changes, renameAttribute(img, "hAlign", "horizontalImageAlign"));

        // vAlign → verticalImageAlign
        addChange(changes, renameAttribute(img, "vAlign", "verticalImageAlign"));

        // horizontalImageAlign enum
        rewriteEnum(img, "horizontalImageAlign", changes,
                new String[]{"Left",   "Left"},
                new String[]{"Center", "Center"},
                new String[]{"Right",  "Right"}
        );

        // verticalImageAlign enum
        rewriteEnum(img, "verticalImageAlign", changes,
                new String[]{"Top",    "Top"},
                new String[]{"Middle", "Middle"},
                new String[]{"Bottom", "Bottom"}
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