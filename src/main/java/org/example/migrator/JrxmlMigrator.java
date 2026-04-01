package org.example.migrator;

import org.apache.maven.plugin.logging.Log;
import org.example.model.MigrationResult;
import org.example.transformer.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Core orchestrator that reads a .jrxml file, applies all registered transformers
 * in order, and writes the migrated XML to the target location.
 */
public class JrxmlMigrator {

    private final Log log;
    private final List<JrxmlTransformer> transformers;

    public JrxmlMigrator(Log log) {
        this.log = log;
        this.transformers = buildTransformerPipeline();
    }

    /**
     * Registers all transformers in the correct application order.
     * Order matters — namespace must be updated before element-level transformers run.
     */
    private List<JrxmlTransformer> buildTransformerPipeline() {
        List<JrxmlTransformer> pipeline = new ArrayList<>();

        // 1. Namespace & schema version — always first
        pipeline.add(new NamespaceAndVersionTransformer());

        // 2. Removed / renamed root-level attributes
        pipeline.add(new RootAttributeTransformer());

        // 3. Font handling — fontName → net.sf.jasperreports.export.* properties
        pipeline.add(new FontTransformer());

        // 4. Chart component migration (JFreeChart → built-in chart API changes)
        pipeline.add(new ChartTransformer());

        // 5. Hyperlink target attribute rename
        pipeline.add(new HyperlinkTransformer());

        // 6. Text element changes (isStretchWithOverflow, markup, printWhenDetailOverflows)
        pipeline.add(new TextElementTransformer());

        // 7. Band splitType default changes and splittingType removal
        pipeline.add(new BandTransformer());

        // 8. Image element changes (onErrorType, isUsingCache)
        pipeline.add(new ImageElementTransformer());

        // 9. Subreport changes (isUsingCache deprecated)
        pipeline.add(new SubreportTransformer());

        // 10. Property / propertyExpression namespace migrations
        pipeline.add(new PropertyNamespaceTransformer());

        // 11. Query language attribute canonicalization
        pipeline.add(new QueryLanguageTransformer());

        // 12. Style / conditional style changes
        pipeline.add(new StyleTransformer());

        // 13. DataSet / group changes
        pipeline.add(new DatasetTransformer());

        // 14. Field description element cleanup
        pipeline.add(new FieldTransformer());

        // 15. Component / generic element namespace updates
        pipeline.add(new ComponentElementTransformer());

        // 16. Print order / orientation enum value renames
        pipeline.add(new EnumValueTransformer());

        // 17. Export configuration properties migration
        pipeline.add(new ExportConfigTransformer());

        return pipeline;
    }

    /**
     * Migrates a single .jrxml file from source to target.
     */
    public MigrationResult migrate(File sourceFile, File targetFile) {
        List<String> changes = new ArrayList<>();

        try {
            // Parse source XML
            SAXBuilder builder = new SAXBuilder();
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
            builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = builder.build(sourceFile);

            // Run each transformer
            for (JrxmlTransformer transformer : transformers) {
                try {
                    List<String> transformerChanges = transformer.transform(document);
                    changes.addAll(transformerChanges);
                } catch (Exception e) {
                    log.warn("Transformer [" + transformer.getName() + "] failed: " + e.getMessage());
                }
            }

            // Ensure parent target directory exists
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            // Write migrated XML
            writeXml(document, targetFile);

            return MigrationResult.success(sourceFile, targetFile, changes);

        } catch (Exception e) {
            return MigrationResult.failure(sourceFile, targetFile,
                    "Parse/write error: " + e.getMessage(), e);
        }
    }

    private void writeXml(Document document, File targetFile) throws IOException {
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        format.setIndent("    ");
        format.setLineSeparator("\n");

        XMLOutputter outputter = new XMLOutputter(format);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            outputter.output(document, fos);
        }
    }
}