# JasperReports 7 Migration Maven Plugin

A Maven plugin that automatically migrates `.jrxml` report files from **JasperReports 6.x** to **JasperReports 7.x**, applying all breaking changes, deprecated API replacements, namespace updates, and attribute renames.

---

## Quick Start

### Build the Plugin

```bash
cd jasper7-migrate-plugin
mvn clean install
```

### Run Migration (Command Line)

```bash
mvn com.jasper.migration:jasper7-migration-maven-plugin:1.0.0:migrate-to-jasper7 \
    -DsourceFolder=/path/to/your/reports \
    -DtargetFolder=/path/to/migrated/reports
```

### Run Migration (Configured in pom.xml)

```xml
<plugin>
    <groupId>com.jasper.migration</groupId>
    <artifactId>jasper7-migration-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>migrate-reports</id>
            <phase>migrate</phase>
            <goals>
                <goal>migrate-to-jasper7</goal>
            </goals>
            <configuration>
                <sourceFolder>${project.basedir}/src/main/reports</sourceFolder>
                <targetFolder>${project.build.directory}/migrated-reports</targetFolder>
                <overwrite>true</overwrite>
                <createBackup>true</createBackup>
                <generateReport>true</generateReport>
                <failOnError>true</failOnError>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Configuration Parameters

| Parameter        | Property          | Default | Description |
|-----------------|-------------------|---------|-------------|
| `sourceFolder`  | `sourceFolder`    | —       | **Required.** Path to folder containing `.jrxml` files. Scanned recursively. |
| `targetFolder`  | `targetFolder`    | —       | **Required.** Output folder. Original directory structure is preserved. |
| `overwrite`     | `overwrite`       | `true`  | Overwrite existing files in target folder. |
| `createBackup`  | `createBackup`    | `false` | Create `.jrxml.bak` alongside each original source file. |
| `failOnError`   | `failOnError`     | `true`  | Fail the build if any file fails to migrate. |
| `generateReport`| `generateReport`  | `true`  | Write a `migration-report.txt` in the target folder. |
| `copyNonJrxml`  | `copyNonJrxml`    | `true`  | Copy non-`.jrxml` files (images, etc.) preserving structure. |
| `skip`          | `skip`            | `false` | Skip the migration entirely. |

---

## What Gets Migrated

### 1. Schema Version
| Change | JR6 | JR7 |
|--------|-----|-----|
| `version` attribute | `6.x.x` | `7.0.0` |
| `xsi:schemaLocation` | `http://...` URL | `https://...` URL |

### 2. Root Element Attribute Renames (`is*` prefix removal)
| JR6 Attribute | JR7 Attribute |
|---------------|---------------|
| `isIgnorePagination` | `ignorePagination` |
| `isTitleNewPage` | `titleNewPage` |
| `isSummaryNewPage` | `summaryNewPage` |
| `isSummaryWithPageHeaderAndFooter` | `summaryWithPageHeaderAndFooter` |
| `isFloatColumnFooter` | `floatColumnFooter` |

### 3. Enum Value Canonicalization
| Attribute | JR6 Values | JR7 Values |
|-----------|------------|------------|
| `printOrder` | `Vertical`, `Horizontal` | `VERTICAL`, `HORIZONTAL` |
| `orientation` | `Portrait`, `Landscape` | `PORTRAIT`, `LANDSCAPE` |
| `whenNoDataType` | `NoPages`, `BlankPage`, `AllSectionsNoDetail`, `NoDataSection` | `NO_PAGES`, `BLANK_PAGE`, `ALL_SECTIONS_NO_DETAIL`, `NO_DATA_SECTION` |
| `positionType` | `Float`, `FixRelativeToTop`, `FixRelativeToBottom` | `FLOAT`, `FIX_RELATIVE_TO_TOP`, `FIX_RELATIVE_TO_BOTTOM` |
| `stretchType` | `NoStretch`, `RelativeToTallestObject`, `RelativeToBandHeight` | `NO_STRETCH`, `RELATIVE_TO_TALLEST_OBJECT`, `RELATIVE_TO_BAND_HEIGHT` |
| `scaleImage` | `Clip`, `FillFrame`, `RetainShape`, `RealHeight`, `RealSize` | `CLIP`, `FILL_FRAME`, `RETAIN_SHAPE`, `REAL_HEIGHT`, `REAL_SIZE` |
| `onErrorType` | `Error`, `Blank`, `Icon` | `ERROR`, `BLANK`, `ICON` |
| `hyperlinkTarget` | `Self`, `Blank`, `Top`, `Parent` | `SELF`, `BLANK`, `TOP`, `PARENT` |

### 4. Font Migration
| Change | Action |
|--------|--------|
| `pdfFontName` attribute | Removed; `fontName` set from mapping |
| `pdfEncoding` attribute | Removed (PDF/A handles encoding automatically) |
| `isPdfEmbedded` attribute | Removed (always embedded in JR7) |
| `<reportFont>` element | Migrated to `<style>` element |

### 5. Text Field Changes
| Change | JR6 | JR7 |
|--------|-----|-----|
| Overflow control | `isStretchWithOverflow="true"` | `textAdjust="StretchHeight"` |
| Overflow control | `isStretchWithOverflow="false"` | `textAdjust="CutText"` |
| Vertical alignment | `verticalAlignment` | `verticalTextAlign` |

### 6. Image Element Changes
| Change | JR6 | JR7 |
|--------|-----|-----|
| Cache control | `isUsingCache="true/false"` | Removed (always cached) |
| Horizontal alignment | `hAlign` | `horizontalImageAlign` |
| Vertical alignment | `vAlign` | `verticalImageAlign` |

### 7. Group Element Changes
| JR6 Attribute | JR7 Attribute |
|---------------|---------------|
| `isStartNewPage` | `startNewPage` |
| `isStartNewColumn` | `startNewColumn` |
| `isResetPageNumber` | `resetPageNumber` |
| `isReprintHeaderOnEachPage` | `reprintHeaderOnEachPage` |

### 8. Band Changes
| Change | Action |
|--------|--------|
| `isSplitAllowed="true"` | Replaced with `splitType="Stretch"` |
| `isSplitAllowed="false"` | Replaced with `splitType="Prevent"` |
| No `splitType` present | Explicit `splitType="Stretch"` added to preserve JR6 default behaviour (JR7 default changed to `Prevent`) |

### 9. Subreport Changes
| Change | Action |
|--------|--------|
| `isUsingCache` attribute | Removed |
| `<returnValue toVariable="...">` | Renamed to `<returnValue variable="...">` |

### 10. Style Element Changes
| Change | Action |
|--------|--------|
| `hAlign` / `hTextAlign` | Renamed to `textAlignment` |
| `vAlign` / `vTextAlign` | Renamed to `verticalTextAlign` |
| `isBlankWhenNull` on `<style>` | Removed (must be on individual elements) |
| Legacy border shorthand attributes | Removed (must use `<box>` child) |
| `<condition>` in `<conditionalStyle>` | Renamed to `<conditionExpression>` |

### 11. Property Name Migrations
| JR6 Property | JR7 Property |
|-------------|--------------|
| `net.sf.jasperreports.export.xls.freeze.row.edge` | `net.sf.jasperreports.export.xls.freeze.row` |
| `net.sf.jasperreports.export.xls.freeze.column.edge` | `net.sf.jasperreports.export.xls.freeze.column` |
| `net.sf.jasperreports.html.class` | `net.sf.jasperreports.export.html.class` |
| `net.sf.jasperreports.html.id` | `net.sf.jasperreports.export.html.id` |
| `net.sf.jasperreports.print.keep.full.snapshot` | **Removed** |
| `net.sf.jasperreports.chart.theme` | **Removed** |

### 12. Chart Migration
| Change | Action |
|--------|--------|
| `chartTheme` attribute on root | Removed |
| `customizerClass` on chart elements | Removed (JFreeChart API gone) |
| `showLegend` missing | Explicit `showLegend="true"` added (JR7 default flipped) |
| `<seriesColors>/<seriesColor>` | Renamed to `<colorSequence>/<color>` |
| `<valueDisplay>` in pie charts | Renamed to `<itemLabel>` |
| Chart plot `orientation` enum | Normalized to `VERTICAL`/`HORIZONTAL` |

### 13. Component Elements
| Change | Action |
|--------|--------|
| `<jr:list printOrder="Vertical">` | Enum value → `VERTICAL` |
| Crosstab `isRepeatColumnHeaders` | Renamed to `repeatColumnHeaders` |
| Crosstab `isRepeatRowHeaders` | Renamed to `repeatRowHeaders` |
| `genericElementType` HTTP namespace | Updated to HTTPS |

### 14. Export Configuration
| Change | Action |
|--------|--------|
| Inline `<jr:xls>` config elements | Converted to `<property>` elements |
| Inline `<jr:html>` config elements | Converted to `<property>` elements |
| PDF/A conformance values (lowercase) | Normalized to `PDFA1A`, `PDFA1B`, etc. |

### 15. Field / Class Attribute
| Change | Action |
|--------|--------|
| Short class names (`String`, `Integer`, etc.) | Expanded to FQN (`java.lang.String`) |
| `fieldValueClass` attribute | Renamed to `class` |

### 16. Query Language
| Change | Action |
|--------|--------|
| `queryString language="SQL"` | Normalized to lowercase `"sql"` |

---

## Migration Report

After each run, a `migration-report.txt` is generated in the target folder:

```
========================================================
  JasperReports 6 → 7 Migration Report
========================================================
  Date          : 2025-01-15 14:32:10
  Source Folder : /reports/src
  Target Folder : /reports/migrated
  Duration      : 234 ms
--------------------------------------------------------
  Total files   : 12
  Succeeded     : 11
  Skipped       : 0
  Errors        : 1
  Total changes : 87
========================================================

FILE: /reports/src/CustomerReport.jrxml
  Status : SUCCESS
  Changes (7):
    • [jasperReport] Updated version from '6.20.0' → '7.0.0'
    • [jasperReport] Renamed isIgnorePagination → ignorePagination
    • [textField] isStretchWithOverflow='true' → textAdjust='StretchHeight'
    • [image] Removed deprecated isUsingCache attribute
    ...
```

---

## Project Structure

```
jasper7-migration-maven-plugin/
├── pom.xml
└── src/
    ├── main/java/com/jasper/migration/
    │   ├── mojo/
    │   │   └── MigrateToJasper7Mojo.java        # Plugin entry point
    │   ├── migrator/
    │   │   └── JrxmlMigrator.java               # Transformation orchestrator
    │   ├── transformer/
    │   │   ├── JrxmlTransformer.java             # Interface
    │   │   ├── AbstractJrxmlTransformer.java     # Base class with helpers
    │   │   ├── NamespaceAndVersionTransformer.java
    │   │   ├── RootAttributeTransformer.java
    │   │   ├── FontTransformer.java
    │   │   ├── ChartTransformer.java
    │   │   ├── HyperlinkTransformer.java
    │   │   ├── TextElementTransformer.java
    │   │   ├── BandTransformer.java
    │   │   ├── ImageElementTransformer.java
    │   │   ├── SubreportTransformer.java
    │   │   ├── PropertyNamespaceTransformer.java
    │   │   ├── QueryLanguageTransformer.java
    │   │   ├── StyleTransformer.java
    │   │   ├── DatasetTransformer.java
    │   │   ├── FieldTransformer.java
    │   │   ├── ComponentElementTransformer.java
    │   │   ├── EnumValueTransformer.java
    │   │   └── ExportConfigTransformer.java
    │   └── model/
    │       ├── MigrationResult.java
    │       └── MigrationReport.java
    └── test/
        ├── java/com/jasper/migration/
        │   └── JrxmlMigratorTest.java
        └── resources/samples/
            └── sample_jr6_report.jrxml
```

---

## Extending the Plugin

To add a new transformation rule:

1. Create a class extending `AbstractJrxmlTransformer`
2. Implement `getName()` and `transform(Document)`
3. Register it in `JrxmlMigrator.buildTransformerPipeline()`

```java
public class MyCustomTransformer extends AbstractJrxmlTransformer {
    @Override
    public String getName() { return "MyCustom"; }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        // apply transformations...
        return changes;
    }
}
```

---

## Notes & Limitations

- The plugin performs **syntactic/structural** migration. Semantic changes (e.g., Java expression logic) must be reviewed manually.
- Chart components migrated to JR7's built-in engine may require visual re-tuning.
- The migration is designed to be **idempotent** — running it twice on an already-migrated file produces no additional changes.
- JR7 introduced Lombok-style API changes in Java report compilation; those are outside JRXML scope.