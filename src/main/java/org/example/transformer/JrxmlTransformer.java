package org.example.transformer;

import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;

/**
 * Contract for every JRXML transformation step.
 */
public interface JrxmlTransformer {

    /**
     * Human-readable name for logging and reports.
     */
    String getName();

    /**
     * Applies the transformation in-place on the document and returns a list
     * of human-readable change descriptions.
     */
    List<String> transform(Document document);
}
