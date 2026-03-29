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

public class RecommendationTrendsChart {

    private static final DateTimeFormatter IN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final Font LABEL_FONT = Font.font("Arial", FontWeight.BOLD, 11);

    private record SeriesEntry(XYChart.Series<String, Number> series, String color) {
    }

    @FunctionalInterface
    private interface TrendGetter {
        int get(RecommendationTrends t);
    }

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
        yAxis.setStyle("-fx-font-family: Arial; -fx-font-size: 12px; -fx-text-fill: #9aa0a6;");

        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle(ticker + " — Analyst Recommendations");
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.RIGHT);
        chart.setAnimated(false);
        chart.setCategoryGap(60);
        chart.setPrefHeight(450);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setStyle(
                "-fx-font-family: Arial;"
                        + "-fx-background-color: #1c1c1c;"
                        + "-fx-plot-background-color: #111111;"
                        + "-fx-chart-title-fill: #e8eaed;"
                        + "-fx-horizontal-grid-line-stroke: #2f2f2f;"
                        + "-fx-vertical-grid-line-stroke: transparent;"
                        + "-fx-tick-label-fill: #9aa0a6;");

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
        wrapper.setStyle("-fx-background-color: #111111;");
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
            if (yAxis.lookup(".axis-label") != null) {
                yAxis.lookup(".axis-label").setStyle(
                        "-fx-font-family: Arial; -fx-font-size: 12px; -fx-text-fill: #9aa0a6;");
            }
            // Dark-theme tick labels
            chart.lookupAll(".tick-label").forEach(n -> n.setStyle("-fx-fill: #9aa0a6; -fx-font-size: 11px;"));
            // Dark chart title
            var titleLbl = chart.lookup(".chart-title");
            if (titleLbl != null)
                titleLbl.setStyle("-fx-text-fill: #e8eaed; -fx-font-size: 14px; -fx-font-weight: bold;");
            // Legend text
            chart.lookupAll(".chart-legend-item").forEach(n -> n.setStyle("-fx-text-fill: #9aa0a6;"));
        }));

        return wrapper;
    }

    private static final List<String> LEGEND_ORDER = List.of("Strong Buy", "Buy", "Hold", "Sell", "Strong Sell");

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

    private static XYChart.Series<String, Number> makeSeries(
            String name, List<String> labels, List<RecommendationTrends> ordered, TrendGetter getter) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < ordered.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), getter.get(ordered.get(i))));
        }
        return series;
    }

    private static String formatPeriod(String period) {
        try {
            return LocalDate.parse(period, IN_FMT).format(OUT_FMT);
        } catch (Exception e) {
            return period;
        }
    }
}
