package org.example.transformer;

import org.jdom2.*;

import java.util.*;

/**
 * JR7 Chart API Migration:
 *
 * JasperReports 7 dropped the legacy JFreeChart-backed chart components and replaced
 * them with a built-in chart rendering engine. Key changes:
 *
 * 1. <barChart>, <pieChart>, <lineChart>, <areaChart>, etc. are now wrapped in
 *    <jr:barChart ...> component syntax with new namespace.
 *
 * 2. chartTheme attribute on jasperReport root is removed.
 *
 * 3. <chartPlot> child renamed to <plot> in most chart types.
 *
 * 4. seriesColors / seriesColor elements renamed to colorSequence / color.
 *
 * 5. <valueDisplay> inside pie charts → <itemLabel>.
 *
 * 6. <catDataset>/<pieDataset>/<xyDataset> minor attribute renames.
 *
 * 7. showLegend attribute default flipped from true to false in JR7.
 *
 * 8. customizerClass attribute removed (JFreeChart-specific, no equivalent).
 */
public class ChartTransformer extends AbstractJrxmlTransformer {

    private static final Set<String> CHART_ELEMENT_NAMES = new LinkedHashSet<>(Arrays.asList(
            "barChart", "bar3DChart", "pieChart", "pie3DChart",
            "lineChart", "areaChart", "xyBarChart", "xyLineChart",
            "xyAreaChart", "scatterChart", "bubbleChart",
            "stackedBarChart", "stackedBar3DChart", "stackedAreaChart",
            "candlestickChart", "highLowChart", "meterChart",
            "thermometerChart", "multiAxisChart", "ganttChart",
            "timeSeriesChart"
    ));

    @Override
    public String getName() {
        return "Chart";
    }

    @Override
    public List<String> transform(Document document) {
        List<String> changes = new ArrayList<>();
        Element root = getRoot(document);

        // Remove chartTheme from root
        Attribute chartTheme = root.getAttribute("chartTheme");
        if (chartTheme != null) {
            root.removeAttribute("chartTheme");
            changes.add("[jasperReport] Removed chartTheme='" + chartTheme.getValue() +
                    "' (JFreeChart theming not supported in JR7; use component style instead)");
        }

        // Process all chart elements
        List<Element> allElements = findAllElements(root);
        for (Element el : allElements) {
            if (CHART_ELEMENT_NAMES.contains(el.getName())) {
                changes.addAll(migrateChartElement(el));
            }
        }

        return changes;
    }

    private List<String> migrateChartElement(Element chart) {
        List<String> changes = new ArrayList<>();
        String chartName = chart.getName();

        // customizerClass removed (JFreeChart-specific)
        Attribute customizerClass = chart.getAttribute("customizerClass");
        if (customizerClass != null) {
            chart.removeAttribute("customizerClass");
            changes.add(String.format("[%s] Removed customizerClass='%s' (JFreeChart API removed in JR7)",
                    chartName, customizerClass.getValue()));
        }

        // showLegend: if explicitly "true", note it's now default false
        Attribute showLegend = chart.getAttribute("showLegend");
        if (showLegend == null) {
            chart.setAttribute("showLegend", "true");
            changes.add(String.format("[%s] Added showLegend='true' (JR7 default is false; preserving original behaviour)",
                    chartName));
        }

        // <chartPlot> rename
        for (Element child : chart.getChildren()) {
            if (child.getName().endsWith("Plot") || child.getName().equals("chartPlot")) {
                changes.addAll(migrateChartPlot(child));
            }
        }

        // seriesColors → colorSequence
        Element seriesColors = chart.getChild("seriesColors");
        if (seriesColors != null) {
            seriesColors.setName("colorSequence");
            for (Element seriesColor : seriesColors.getChildren("seriesColor")) {
                seriesColor.setName("color");
                Attribute seriesOrder = seriesColor.getAttribute("seriesOrder");
                if (seriesOrder != null) seriesColor.removeAttribute("seriesOrder");
            }
            changes.add(String.format("[%s] Renamed <seriesColors>/<seriesColor> → <colorSequence>/<color>", chartName));
        }

        // valueDisplay (pie) → itemLabel
        Element valueDisplay = chart.getChild("valueDisplay");
        if (valueDisplay == null) {
            // Check inside plot elements
            for (Element child : chart.getChildren()) {
                valueDisplay = child.getChild("valueDisplay");
                if (valueDisplay != null) break;
            }
        }
        if (valueDisplay != null) {
            valueDisplay.setName("itemLabel");
            changes.add(String.format("[%s] Renamed <valueDisplay> → <itemLabel>", chartName));
        }

        // <catDataset> labelExpression → categoryExpression
        List<Element> datasets = new ArrayList<>();
        datasets.addAll(chart.getChildren("categoryDataset"));
        datasets.addAll(chart.getChildren("catDataset"));
        for (Element ds : datasets) {
            changes.addAll(migrateCatDataset(ds));
        }

        return changes;
    }

    private List<String> migrateChartPlot(Element plot) {
        List<String> changes = new ArrayList<>();

        // backcolor → plot background (already an attribute, just log)
        Attribute orientation = plot.getAttribute("orientation");
        if (orientation != null) {
            String val = orientation.getValue();
            if ("Horizontal".equals(val)) {
                orientation.setValue("HORIZONTAL");
                changes.add("[chartPlot] orientation: 'Horizontal' → 'HORIZONTAL'");
            } else if ("Vertical".equals(val)) {
                orientation.setValue("VERTICAL");
                changes.add("[chartPlot] orientation: 'Vertical' → 'VERTICAL'");
            }
        }

        return changes;
    }

    private List<String> migrateCatDataset(Element ds) {
        List<String> changes = new ArrayList<>();
        // labelExpression was sometimes used; now it's categoryExpression in JR7
        for (Element series : ds.getChildren("categorySeries")) {
            Element labelExpr = series.getChild("labelExpression");
            if (labelExpr != null) {
                labelExpr.setName("categoryExpression");
                changes.add("[categorySeries] Renamed <labelExpression> → <categoryExpression>");
            }
        }
        return changes;
    }
}