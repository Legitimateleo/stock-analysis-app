package team2.parallax.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import team2.parallax.service.PolygonCandleService;
import team2.parallax.service.PolygonCandleService.CandleResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A clean stock price LineChart styled to match the Google Finance dark theme.
 * Supports 1D / 5D / 1M / 6M / 1Y timeframes via the Polygon.io Aggregates API.
 *
 * <p>Chart type: {@link LineChart} — a single price line with no area fill.
 *
 * <p>X-axis tick spacing per period:
 * <pre>
 *  1D  → every 30 minutes  (9:30 AM, 10:00 AM, 10:30 AM …)
 *  5D  → every 1 day       (Mon 3/23, Tue 3/24 …)
 *  1M  → every 1 week      (Mar 1, Mar 8, Mar 15 …)
 *  6M  → every 1 month     (Oct, Nov, Dec …)
 *  1Y  → every 2 weeks     (Jan '25, Feb '25 …)
 * </pre>
 */
public class StockChart {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String BG      = "#111111";
    private static final String GREEN   = "#34a853";
    private static final String RED     = "#ea4335";
    private static final String SEC     = "#9aa0a6";
    private static final String DIVIDER = "#2f2f2f";
    private static final String BTN_ACT = "#3c4043";

    // ── Time constants (seconds) ──────────────────────────────────────────────
    private static final long HALF_HOUR = 1_800L;
    private static final long ONE_HOUR  = 3_600L;
    private static final long ONE_DAY   = 86_400L;
    private static final long ONE_WEEK  = 7 * ONE_DAY;

    // ── State ─────────────────────────────────────────────────────────────────
    private final PolygonCandleService polygonService;
    private final String symbol;

    private LineChart<Number, Number> chart;
    private NumberAxis xAxis;
    private Button activeBtn;

    /**
     * Set to false when the containing panel replaces this chart instance.
     * All in-flight Platform.runLater callbacks check this first to avoid
     * touching nodes that have already been removed from the scene graph.
     */
    private volatile boolean active = true;

    public StockChart(PolygonCandleService polygonService, String symbol) {
        this.polygonService = polygonService;
        this.symbol         = symbol;
    }

    /** Call this before discarding the chart to cancel pending callbacks. */
    public void cancel() { active = false; }

    // =========================================================================
    //  PUBLIC – build the full chart VBox
    // =========================================================================
    public VBox build() {

        // ── X-axis ────────────────────────────────────────────────────────────
        xAxis = new NumberAxis();
        xAxis.setAutoRanging(true);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickMarkVisible(true);
        xAxis.setTickLength(4);
        xAxis.setTickLabelFormatter(timeFormatter("1D"));

        // ── Y-axis: price on the right ────────────────────────────────────────
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setSide(javafx.geometry.Side.RIGHT);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) {
                return n == null ? "" : String.format("%.0f", n.doubleValue());
            }
            @Override public Number fromString(String s) { return 0; }
        });

        // ── LineChart (no area fill – just the price line) ────────────────────
        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);   // no dot markers on data points
        chart.setPrefHeight(260);
        chart.setMinHeight(200);
        chart.setPadding(new Insets(4, 0, 0, 0));
        styleChartBase();

        // ── Timeframe buttons ─────────────────────────────────────────────────
        String[] periods = { "1D", "1M", "6M", "1Y" };
        HBox btnRow = new HBox(4);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(8, 0, 6, 2));
        btnRow.setStyle("-fx-background-color: " + BG + ";");

        for (String p : periods) {
            Button btn = buildPeriodBtn(p);
            btnRow.getChildren().add(btn);
            if ("1D".equals(p)) {
                activeBtn = btn;
                setActive(btn);
            }
        }

        VBox container = new VBox(0, btnRow, chart);
        container.setStyle("-fx-background-color: " + BG + ";");

        Platform.runLater(() -> loadData("1D"));
        return container;
    }

    // =========================================================================
    //  PRIVATE – button factory
    // =========================================================================
    private Button buildPeriodBtn(String period) {
        Button btn = new Button(period);
        setInactive(btn);

        btn.setOnAction(e -> {
            if (activeBtn != null && activeBtn != btn) setInactive(activeBtn);
            activeBtn = btn;
            setActive(btn);
            loadData(period);
        });
        btn.setOnMouseEntered(e -> { if (btn != activeBtn) btn.setStyle(btnStyle(BTN_ACT, "#c5c8cc", false)); });
        btn.setOnMouseExited (e -> { if (btn != activeBtn) setInactive(btn); });
        return btn;
    }

    private void setActive  (Button b) { b.setStyle(btnStyle(BTN_ACT,      "#e8eaed", true));  }
    private void setInactive(Button b) { b.setStyle(btnStyle("transparent", SEC,       false)); }

    private String btnStyle(String bg, String fg, boolean bold) {
        return "-fx-background-color: " + bg + ";"
                + "-fx-text-fill: " + fg + ";"
                + "-fx-font-size: 12px;"
                + (bold ? "-fx-font-weight: bold;" : "")
                + "-fx-padding: 4px 12px;"
                + "-fx-background-radius: 4px;"
                + "-fx-cursor: hand;";
    }

    // =========================================================================
    //  PRIVATE – data loading
    // =========================================================================
    private void loadData(String period) {
        final String per = period;

        Task<XYChart.Series<Number, Number>> task = new Task<>() {
            @Override
            protected XYChart.Series<Number, Number> call() {
                CandleResult candles = polygonService.getCandles(symbol, per);
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                if (candles.isEmpty()) return series;

                long[]   times  = candles.timestamps();
                double[] closes = candles.closes();

                // Polygon timestamps are epoch-milliseconds → convert to epoch-seconds
                for (int i = 0; i < times.length; i++) {
                    series.getData().add(new XYChart.Data<>(times[i] / 1_000L, closes[i]));
                }
                return series;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (!active) return;

            XYChart.Series<Number, Number> series = task.getValue();
            if (series.getData().isEmpty()) return;

            xAxis.setTickLabelFormatter(timeFormatter(per));
            configureXAxis(per, series);

            if (chart.getData().isEmpty()) {
                chart.getData().add(series);
            } else {
                chart.getData().set(0, series);
            }

            Platform.runLater(() -> { if (active) styleLineColor(series); });
        }));

        task.setOnFailed(ignored -> {});
        new Thread(task).start();
    }

    // =========================================================================
    //  PRIVATE – x-axis configuration
    // =========================================================================

    /**
     * Fixes x-axis tick spacing per timeframe so labels land on meaningful
     * calendar boundaries:
     *
     * <ul>
     *   <li>1D  – tick every 30 min, bounds snapped to half-hour marks</li>
     *   <li>5D  – tick every 1 day,  bounds snapped to midnight UTC</li>
     *   <li>1M  – tick every 1 week, bounds snapped to nearest day</li>
     *   <li>6M  – tick every 1 month (≈30 days)</li>
     *   <li>1Y  – tick every 2 weeks (≈14 days)</li>
     * </ul>
     */
    private void configureXAxis(String period, XYChart.Series<Number, Number> series) {
        var data = series.getData();
        if (data.isEmpty()) { xAxis.setAutoRanging(true); return; }

        long first = data.get(0).getXValue().longValue();
        long last  = data.get(data.size() - 1).getXValue().longValue();

        switch (period) {
            case "1D" -> {
                // 30-minute ticks, exact half-hour boundaries
                long lo = (first / HALF_HOUR) * HALF_HOUR;
                long hi = ((last + HALF_HOUR - 1) / HALF_HOUR) * HALF_HOUR;
                setAxisBounds(lo, hi, HALF_HOUR);
            }
            case "1M" -> {
                // One tick per week
                long lo = (first / ONE_DAY) * ONE_DAY;
                long hi = ((last  / ONE_DAY) + 1) * ONE_DAY;
                setAxisBounds(lo, hi, ONE_WEEK);
            }
            case "6M" -> {
                // One tick per month (≈30 days)
                long lo = (first / ONE_DAY) * ONE_DAY;
                long hi = ((last  / ONE_DAY) + 1) * ONE_DAY;
                setAxisBounds(lo, hi, 30 * ONE_DAY);
            }
            case "1Y" -> {
                // One tick every 2 weeks
                long lo = (first / ONE_DAY) * ONE_DAY;
                long hi = ((last  / ONE_DAY) + 1) * ONE_DAY;
                setAxisBounds(lo, hi, 2 * ONE_WEEK);
            }
            default -> xAxis.setAutoRanging(true);
        }
    }

    private void setAxisBounds(long lo, long hi, long tickUnit) {
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(lo);
        xAxis.setUpperBound(hi);
        xAxis.setTickUnit(tickUnit);
    }

    // =========================================================================
    //  PRIVATE – styling helpers
    // =========================================================================

    private void styleChartBase() {
        chart.setStyle(
                "-fx-background-color: " + BG + ";"
                + "-fx-plot-background-color: " + BG + ";");
        Platform.runLater(() -> {
            if (!active) return;
            chart.lookupAll(".chart-plot-background")
                    .forEach(n -> n.setStyle("-fx-background-color: " + BG + ";"));
            chart.lookupAll(".chart-horizontal-grid-lines")
                    .forEach(n -> n.setStyle("-fx-stroke: " + DIVIDER + ";"));
            chart.lookupAll(".chart-vertical-grid-lines")
                    .forEach(n -> n.setStyle("-fx-stroke: transparent;"));
            chart.lookupAll(".axis")
                    .forEach(n -> n.setStyle(
                            "-fx-tick-label-fill: " + SEC + ";"
                            + "-fx-font-size: 10px;"
                            + "-fx-base: " + BG + ";"));
            chart.lookupAll(".axis-line")
                    .forEach(n -> n.setStyle("-fx-stroke: " + DIVIDER + ";"));
        });
    }

    /** Colors the chart line green (up) or red (down) based on price direction. */
    private void styleLineColor(XYChart.Series<Number, Number> series) {
        var data = series.getData();
        boolean up = data.size() < 2 ||
                data.get(data.size() - 1).getYValue().doubleValue()
                        >= data.get(0).getYValue().doubleValue();
        String color = up ? GREEN : RED;

        // LineChart CSS node is ".chart-series-line" (not ".chart-series-area-line")
        var line = chart.lookup(".chart-series-line");
        if (line != null)
            line.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px;");

        // Re-apply axis label colour (JavaFX resets it after data changes)
        chart.lookupAll(".axis").forEach(n -> n.setStyle(
                "-fx-tick-label-fill: " + SEC + ";"
                + "-fx-font-size: 10px;"
                + "-fx-base: " + BG + ";"));
    }

    // =========================================================================
    //  PRIVATE – x-axis label formatter
    // =========================================================================

    /** Returns a {@link StringConverter} that formats epoch-second timestamps. */
    private StringConverter<Number> timeFormatter(String period) {
        final ZoneId ET = ZoneId.of("America/New_York");
        return new StringConverter<>() {
            final DateTimeFormatter fmt = switch (period) {
                // 1D  → clock time in ET:   9:30 AM, 10:00 AM …
                case "1D" -> DateTimeFormatter.ofPattern("h:mm a").withZone(ET);
                // 1M  → month + day:  Mar 1, Mar 8, Mar 15 …
                case "1M" -> DateTimeFormatter.ofPattern("MMM d").withZone(ET);
                // 6M  → abbreviated month:  Oct, Nov, Dec …
                case "6M" -> DateTimeFormatter.ofPattern("MMM").withZone(ET);
                // 1Y  → month + short year: Jan '25, Mar '25 …
                default   -> DateTimeFormatter.ofPattern("MMM ''yy").withZone(ET);
            };

            @Override
            public String toString(Number n) {
                if (n == null || n.longValue() <= 0) return "";
                try {
                    return fmt.format(Instant.ofEpochSecond(n.longValue()));
                } catch (Exception e) {
                    return "";
                }
            }

            @Override
            public Number fromString(String s) { return 0; }
        };
    }
}
