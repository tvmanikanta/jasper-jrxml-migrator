package org.example.transformer;

import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;

/**
 * Abstract base providing XML traversal helpers for concrete transformers.
 */
public abstract class AbstractJrxmlTransformer implements JrxmlTransformer {

    protected static final String JASPER_NS_6 = "http://jasperreports.sourceforge.net/jasperreports";
    protected static final String JASPER_NS_7 = "http://jasperreports.sourceforge.net/jasperreports";

    // JR7 introduced a separate component namespace
    protected static final String JASPER_COMPONENTS_NS_6 = "http://jasperreports.sourceforge.net/jasperreports/components";
    protected static final String JASPER_COMPONENTS_NS_7 = "http://jasperreports.sourceforge.net/jasperreports/components";

    /**
     * Returns the root jasperReport element.
     */
    protected Element getRoot(Document document) {
        return document.getRootElement();
    }

    /**
     * Recursively collects all elements with the given local name.
     */
    protected List<Element> findElements(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        collectElements(parent, localName, result);
        return result;
    }

    private void collectElements(Element element, String localName, List<Element> result) {
        if (element.getName().equals(localName)) {
            result.add(element);
        }
        for (Element child : element.getChildren()) {
            collectElements(child, localName, result);
        }
    }

    /**
     * Recursively collects ALL elements in the document.
     */
    protected List<Element> findAllElements(Element parent) {
        List<Element> result = new ArrayList<>();
        collectAllElements(parent, result);
        return result;
    }

    private void collectAllElements(Element element, List<Element> result) {
        result.add(element);
        for (Element child : element.getChildren()) {
            collectAllElements(child, result);
        }
    }

    /**
     * Renames an attribute on an element if it exists.
     * Returns a change description or null.
     */
    protected String renameAttribute(Element element, String oldName, String newName) {
        Attribute attr = element.getAttribute(oldName);
        if (attr != null) {
            String value = attr.getValue();
            element.removeAttribute(oldName);
            element.setAttribute(newName, value);
            return String.format("[%s] Renamed attribute '%s' → '%s' (value='%s')",
                    element.getName(), oldName, newName, value);
        }
        return null;
    }

    /**
     * Removes an attribute if it exists. Returns change description or null.
     */
    protected String removeAttribute(Element element, String attrName, String reason) {
        Attribute attr = element.getAttribute(attrName);
        if (attr != null) {
            String value = attr.getValue();
            element.removeAttribute(attrName);
            return String.format("[%s] Removed deprecated attribute '%s'='%s' (%s)",
                    element.getName(), attrName, value, reason);
        }
        return null;
    }

    /**
     * Sets an attribute to a new value if its current value matches oldValue.
     */
    protected String rewriteAttributeValue(Element element, String attrName,
                                           String oldValue, String newValue) {
        Attribute attr = element.getAttribute(attrName);
        if (attr != null && oldValue.equals(attr.getValue())) {
            attr.setValue(newValue);
            return String.format("[%s] Changed %s='%s' → '%s'",
                    element.getName(), attrName, oldValue, newValue);
        }
        return null;
    }

    /**
     * Adds a <property> child element if not already present.
     */
    protected String addPropertyElement(Element parent, String name, String value) {
        // Check if property already present
        for (Element child : parent.getChildren("property")) {
            if (name.equals(child.getAttributeValue("name"))) {
                return null; // already exists
            }
        }
        Element prop = new Element("property");
        prop.setAttribute("name", name);
        prop.setAttribute("value", value);
        // Insert as first child (JR convention)
        parent.addContent(0, prop);
        return String.format("[%s] Added <property name='%s' value='%s'/>",
                parent.getName(), name, value);
    }

    /**
     * Collects non-null strings into the changes list.
     */
    protected void addChange(List<String> changes, String change) {
        if (change != null) {
            changes.add(change);
        }
    }

    /**
     * Processes every element with the given name across the entire document.
     */
    protected List<String> processAllNamed(Document document, String elementName,
                                           java.util.function.Function<Element, List<String>> processor) {
        List<String> changes = new ArrayList<>();
        List<Element> elements = findElements(getRoot(document), elementName);
        for (Element el : elements) {
            List<String> elChanges = processor.apply(el);
            if (elChanges != null) changes.addAll(elChanges);
        }
        return changes;
    }
}