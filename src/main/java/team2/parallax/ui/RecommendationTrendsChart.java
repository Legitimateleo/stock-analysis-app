package team2.parallax.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import team2.parallax.model.RecommendationTrends;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecommendationTrendsChart is a static factory class that builds a styled
 * JavaFX {@link StackedBarChart} visualizing analyst recommendation consensus
 * data for a given stock over the most recent four months.
 *
 * <p>This class is stateless — all functionality is exposed through the single
 * static factory method {@link #build(List, String)}, which constructs and
 * returns a fully configured {@link Pane} containing the chart. It is called
 * by {@code MainWindow.onTrendsLoaded()} when the user clicks the Show Trends
 * button and the data has been successfully fetched.</p>
 *
 * <p>The chart displays five recommendation categories stacked per month:
 * Strong Buy, Buy, Hold, Sell, and Strong Sell. Each category is color-coded
 * using the same green-to-red sentiment spectrum used by the valuation signal
 * labels in the main results panel. Numeric analyst counts are rendered as
 * floating labels beside each bar segment for readability.</p>
 *
 * <p>The chart binds its width to its parent wrapper {@link Pane} and refreshes
 * bar colors and floating labels dynamically on resize via a width change
 * listener. This is necessary because JavaFX applies default series colors
 * after layout, which would override the custom color scheme without the
 * post-layout refresh.</p>
 *
 * @see team2.parallax.model.RecommendationTrends
 * @see MainWindow
 */

public class RecommendationTrendsChart {
    /**
     * Input date format expected from the Finnhub API response.
     * Finnhub returns recommendation period dates as {@code "yyyy-MM-dd"}.
     */
    private static final DateTimeFormatter IN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * Output date format used for X-axis category labels on the chart.
     * Periods are displayed as {@code "MMM yyyy"} (e.g. {@code "Mar 2025"}).
     */
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("MMM yyyy");
    /**
     * Output date format used for X-axis category labels on the chart.
     * Periods are displayed as {@code "MMM yyyy"} (e.g. {@code "Mar 2025"}).
     */
    private static final Font LABEL_FONT = Font.font("Arial", FontWeight.BOLD, 11);

    /**
     * Internal record pairing a chart data series with its hex color string.
     * Used to apply consistent custom colors to bar segments and legend items
     * after JavaFX layout completes.
     *
     * @param series the {@link XYChart.Series} containing the chart data.
     * @param color  the hex color string for this series (e.g. {@code "#27ae60"}).
     */
    private record SeriesEntry(XYChart.Series<String, Number> series, String color) {
    }


    @FunctionalInterface
    private interface TrendGetter {
        int get(RecommendationTrends t);
    }
    /**
     * Builds and returns a fully configured {@link Pane} containing a styled
     * stacked bar chart of analyst recommendation trends for the given stock.
     *
     * <p>The chart displays up to four months of data, reversed so the most
     * recent month appears on the right. Only recommendation categories with
     * at least one non-zero value across the displayed months are included
     * as series, keeping the chart uncluttered when data is sparse.</p>
     *
     * <p>Bar colors and floating count labels are applied after JavaFX layout
     * via {@code Platform.runLater()} and a width change listener, as JavaFX
     * overwrites custom node styles during its initial chart rendering pass.</p>
     *
     * @param trends the list of {@link RecommendationTrends} objects returned
     *               by {@code MarketDataService.getTrends()}. The first entry
     *               is the most recent month. Up to four entries are displayed.
     * @param ticker the stock ticker symbol displayed in the chart title
     *               (e.g. {@code "AAPL"}).
     * @return a {@link Pane} containing the fully configured
     *         {@link StackedBarChart}, ready for insertion into the
     *         {@code MainWindow} results panel.
     */
    public static Pane build(List<RecommendationTrends> trends, String ticker) {
        int count = Math.min(4, trends.size());
        List<RecommendationTrends> ordered = new ArrayList<>(trends.subList(0, count));
        Collections.reverse(ordered);

        List<String> labels = ordered.stream().map(t -> formatPeriod(t.getPeriod())).toList();

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setCategories(FXCollections.observableArrayList(labels));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("# Analysts");
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFont(Font.font("Arial", 12));
        yAxis.setStyle("-fx-font-family: Arial; -fx-font-size: 12px; -fx-text-fill: #888888;");

        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle(ticker + " Recommendation Trends");
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.RIGHT);
        chart.setAnimated(false);
        chart.setCategoryGap(60);
        chart.setPrefHeight(450);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setStyle("-fx-font-family: Arial;");

        record Def(String name, String color, TrendGetter getter) {
        }
        List<Def> defs = List.of(
                new Def("Strong Sell", "#7a2020", RecommendationTrends::getStrongSell),
                new Def("Sell", "#c0392b", RecommendationTrends::getSell),
                new Def("Hold", "#c9960c", RecommendationTrends::getHold),
                new Def("Buy", "#27ae60", RecommendationTrends::getBuy),
                new Def("Strong Buy", "#1a5c1a", RecommendationTrends::getStrongBuy));

        List<SeriesEntry> entries = new ArrayList<>();
        for (Def def : defs) {
            if (ordered.stream().anyMatch(t -> def.getter().get(t) > 0)) {
                entries.add(new SeriesEntry(makeSeries(def.name(), labels, ordered, def.getter()), def.color()));
            }
        }

        chart.getData().addAll(entries.stream().map(SeriesEntry::series).toList());

        Pane wrapper = new Pane();
        wrapper.setPrefHeight(450);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        chart.prefWidthProperty().bind(wrapper.widthProperty());
        chart.prefHeightProperty().bind(wrapper.heightProperty());
        wrapper.getChildren().add(chart);

        chart.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            refreshLabels(wrapper, entries);
            refreshLegend(chart, entries);
        }));
        Platform.runLater(() -> Platform.runLater(() -> {
            refreshLabels(wrapper, entries);
            refreshLegend(chart, entries);
            yAxis.lookup(".axis-label").setStyle(
                    "-fx-font-family: Arial; -fx-font-size: 12px; -fx-text-fill: #888888;");
        }));

        return wrapper;
    }

    private static final List<String> LEGEND_ORDER = List.of("Strong Buy", "Buy", "Hold", "Sell", "Strong Sell");
    /**
     * Sorts and recolors the chart legend items into the desired display order
     * defined by {@link #LEGEND_ORDER}, then applies the custom hex color to
     * each legend symbol. Called after layout since JavaFX renders the legend
     * in series insertion order with default colors by default.
     *
     * @param chart   the {@link StackedBarChart} whose legend should be refreshed.
     * @param entries the list of {@link SeriesEntry} objects providing the
     *                series name-to-color mapping.
     */
    private static void refreshLegend(StackedBarChart<String, Number> chart, List<SeriesEntry> entries) {
        var legend = chart.lookup(".chart-legend");
        if (!(legend instanceof javafx.scene.layout.Pane legendPane))
            return;

        java.util.Map<String, String> colorMap = new java.util.HashMap<>();
        for (SeriesEntry e : entries)
            colorMap.put(e.series().getName(), e.color());

        // Sort legend items into desired display order (idempotent)
        var items = new ArrayList<>(legendPane.getChildren());
        items.sort((a, b) -> {
            String ta = a instanceof javafx.scene.control.Label l ? l.getText() : "";
            String tb = b instanceof javafx.scene.control.Label l ? l.getText() : "";
            int ia = LEGEND_ORDER.indexOf(ta);
            if (ia < 0)
                ia = 99;
            int ib = LEGEND_ORDER.indexOf(tb);
            if (ib < 0)
                ib = 99;
            return Integer.compare(ia, ib);
        });
        legendPane.getChildren().setAll(items);

        // Style each symbol by its label name
        for (var item : items) {
            if (!(item instanceof javafx.scene.control.Label lbl))
                continue;
            String color = colorMap.get(lbl.getText());
            if (color == null)
                continue;
            var sym = lbl.lookup(".chart-legend-item-symbol");
            if (sym != null)
                sym.setStyle("-fx-background-color: " + color + ";");
        }
    }
    /**
     * Removes all existing floating count labels from the wrapper pane and
     * redraws them beside each visible bar segment. Also applies the custom
     * hex bar color to each segment node. Called after layout and on resize.
     *
     * <p>Bar segments with a value of {@code 0} are hidden. Labels are
     * positioned to the right of each bar segment using scene-to-local
     * coordinate conversion.</p>
     *
     * @param wrapper the {@link Pane} wrapping the chart, used as the
     *                coordinate space for floating label positioning.
     * @param entries the list of {@link SeriesEntry} objects providing
     *                series data and color information.
     */
    private static void refreshLabels(Pane wrapper, List<SeriesEntry> entries) {
        wrapper.getChildren().removeIf(n -> n instanceof Label);

        for (SeriesEntry entry : entries) {
            for (XYChart.Data<String, Number> data : entry.series().getData()) {
                if (data.getNode() == null)
                    continue;

                int value = data.getYValue().intValue();
                if (value == 0) {
                    data.getNode().setVisible(false);
                    continue;
                }

                data.getNode().setStyle("-fx-bar-fill: " + entry.color() + ";");

                Bounds bar = wrapper.sceneToLocal(data.getNode().localToScene(data.getNode().getBoundsInLocal()));
                if (bar.getWidth() <= 0 || bar.getHeight() <= 0)
                    continue;

                Label lbl = new Label(String.valueOf(value));
                lbl.setFont(LABEL_FONT);
                lbl.setStyle("-fx-text-fill: " + entry.color() + ";");
                lbl.setMouseTransparent(true);
                lbl.setLayoutX(bar.getMaxX() + 4);
                lbl.setLayoutY(bar.getMinY() + bar.getHeight() / 2 - 7);

                wrapper.getChildren().add(lbl);
            }
        }
    }

    /**
     * Builds a single {@link XYChart.Series} for one recommendation category
     * by iterating the ordered trends list and extracting values using the
     * provided {@link TrendGetter}.
     *
     * @param name    the display name of this series (e.g. {@code "Strong Buy"}).
     * @param labels  the list of formatted X-axis category labels corresponding
     *                to each entry in {@code ordered}.
     * @param ordered the ordered list of {@link RecommendationTrends} entries
     *                to extract data from.
     * @param getter  the {@link TrendGetter} used to extract the integer count
     *                for this category from each trends entry.
     * @return a fully populated {@link XYChart.Series} ready for insertion
     *         into the chart.
     */
    private static XYChart.Series<String, Number> makeSeries(
            String name, List<String> labels, List<RecommendationTrends> ordered, TrendGetter getter) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < ordered.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), getter.get(ordered.get(i))));
        }
        return series;
    }

    /**
     * Converts a Finnhub recommendation period date string from
     * {@code "yyyy-MM-dd"} format to {@code "MMM yyyy"} format for
     * display on the chart X-axis.
     *
     * @param period the raw period string returned by the Finnhub API
     *               (e.g. {@code "2025-03-01"}).
     * @return the formatted period string (e.g. {@code "Mar 2025"}),
     *         or the original string if parsing fails.
     */
    private static String formatPeriod(String period) {
        try {
            return LocalDate.parse(period, IN_FMT).format(OUT_FMT);
        } catch (Exception e) {
            return period;
        }
    }
}
