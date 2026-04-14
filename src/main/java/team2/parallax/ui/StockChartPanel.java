package team2.parallax.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import team2.parallax.api.ChartDataClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StockChartPanel extends VBox {

    // ── Canvas geometry ───────────────────────────────────────────────────────
    private static final double CW = 740;
    private static final double CH = 250;
    private static final double PL = 70; // left – Y-axis labels
    private static final double PR = 16; // right
    private static final double PT = 14; // top
    private static final double PB = 34; // bottom – X-axis labels
    private static final double PLOT_W = CW - PL - PR; // ≈ 654
    private static final double PLOT_H = CH - PT - PB; // ≈ 202

    private static final ZoneId ET = ZoneId.of("America/New_York");

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG = Color.web("#181c27");
    private static final Color GRID = Color.web("#2c3347");
    private static final Color AXIS_TXT = Color.web("#7e8fa8");
    private static final Color UP = Color.web("#64C861"); // bright green – price rising
    private static final Color DOWN = Color.web("#ef5350"); // red – price falling
    private static final Color XHAIR = Color.web("#6b7a9580"); // semi-transparent crosshair
    private static final Color TIP_BG = Color.web("#1e2535f0"); // tooltip background

    // ── State ─────────────────────────────────────────────────────────────────
    private final ChartDataClient polygon;

    private String currentTicker = null;
    private String activeTimeframe = "1M";

    private List<Double> prices = new ArrayList<>();
    private List<Long> timestamps = new ArrayList<>(); // epoch-milliseconds

    /** Pre-computed pixel positions – refreshed whenever data changes. */
    private double[] px;
    private double[] py;

    /** Padded Y-axis bounds. */
    private double axisLo;
    private double axisHi;

    private Color lineColor = UP;
    private int hoverIdx = -1;

    /** In-memory result cache: key = "TICKER-TIMEFRAME" */
    private final Map<String, double[]> cacheClose = new HashMap<>(); // Rendered subset
    private final Map<String, long[]> cacheTs = new HashMap<>();
    private final Map<String, double[]> cacheFullClose = new HashMap<>(); // Full padded history

    public interface TimeframeChangeListener {
        void onDataLoaded(String timeframe, double oldPrice, double currentPrice);
    }

    private TimeframeChangeListener listener;

    // ── UI ────────────────────────────────────────────────────────────────────
    private final HBox tabBar;
    private final Canvas canvas;
    private final Label statusLbl;

    // ─────────────────────────────────────────────────────────────────────────
    public StockChartPanel(ChartDataClient polygon) {
        this.polygon = polygon;

        setSpacing(0);
        setPadding(new Insets(0));
        setStyle("-fx-background-color: #181c27; -fx-background-radius: 8;");

        tabBar = buildTabBar();
        canvas = new Canvas(CW, CH);
        statusLbl = new Label("");
        statusLbl.setStyle(
                "-fx-text-fill: #7e8fa8; -fx-font-size: 11px;"
                        + "-fx-padding: 0 0 4 " + (int) PL + ";");

        canvas.setOnMouseMoved(e -> {
            if (px == null || px.length < 2)
                return;
            int newIdx = nearestIndex(e.getX());
            if (e.getX() < PL || e.getX() > PL + PLOT_W
                    || e.getY() < PT || e.getY() > PT + PLOT_H) {
                newIdx = -1;
            }
            if (newIdx != hoverIdx) {
                hoverIdx = newIdx;
                repaint();
            }
        });
        canvas.setOnMouseExited(e -> {
            if (hoverIdx != -1) {
                hoverIdx = -1;
                repaint();
            }
        });
        canvas.setCursor(Cursor.CROSSHAIR);

        paintEmpty("Search for a stock to view its chart");
        getChildren().addAll(tabBar, canvas, statusLbl);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void load(String ticker) {
        if (!ticker.equals(currentTicker)) {
            cacheClose.clear();
            cacheTs.clear();
            cacheFullClose.clear();
            currentTicker = ticker;
        }
        fetchOrLoad(activeTimeframe);
    }

    public void setTimeframeChangeListener(TimeframeChangeListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null && !prices.isEmpty()) {
            listener.onDataLoaded(activeTimeframe, prices.get(0), prices.get(prices.size() - 1));
        }
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private HBox buildTabBar() {
        HBox bar = new HBox(35); // Increased spacing to move SMA section to the right
        bar.setPadding(new Insets(8, 8, 8, 8));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #181c27;");

        HBox timeframesBox = new HBox(0);
        timeframesBox.setPadding(new Insets(2, 6, 2, 6));
        timeframesBox.setStyle(
                "-fx-background-color: #1e2535; -fx-background-radius: 6; -fx-border-color: #2c3347; -fx-border-radius: 6;");
        for (String tf : List.of("1D", "5D", "1M", "3M", "6M", "1Y", "2Y")) {
            timeframesBox.getChildren().add(makeTabLabel(tf));
        }

        bar.getChildren().addAll(timeframesBox);

        return bar;
    }

    private Label makeTabLabel(String tf) {
        Label lbl = new Label(tf);
        lbl.setPadding(new Insets(6, 14, 6, 14));
        lbl.setCursor(Cursor.HAND);
        lbl.setId("chart-tab-" + tf);
        styleTab(lbl, tf.equals(activeTimeframe));
        lbl.setOnMouseClicked(e -> {
            if (!tf.equals(activeTimeframe)) {
                activeTimeframe = tf;
                refreshTabStyles();
                if (currentTicker != null)
                    fetchOrLoad(tf);
            }
        });
        return lbl;
    }

    private void styleTab(Label lbl, boolean active) {
        if (active) {
            lbl.setStyle(
                    "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;"
                            + "-fx-border-color: transparent transparent #64C861 transparent;"
                            + "-fx-border-width: 0 0 2.5 0;"
                            + "-fx-background-color: transparent;");
        } else {
            lbl.setStyle(
                    "-fx-text-fill: #7e8fa8; -fx-font-size: 12px;"
                            + "-fx-border-color: transparent;"
                            + "-fx-background-color: transparent;");
        }
    }

    private void refreshTabStyles() {
        refreshNode(tabBar);
    }

    private void refreshNode(javafx.scene.Node node) {
        if (node instanceof Label lbl && lbl.getId() != null && lbl.getId().startsWith("chart-tab-")) {
            styleTab(lbl, lbl.getText().equals(activeTimeframe));
        } else if (node instanceof javafx.scene.layout.Pane pane) {
            for (javafx.scene.Node child : pane.getChildren()) {
                refreshNode(child);
            }
        }
    }

    // ── Trading-day helpers ───────────────────────────────────────────────────

    /**
     * Returns the most recent US trading weekday (Mon–Fri) in Eastern Time.
     * Skips Saturday and Sunday; does not account for US public holidays.
     */
    private LocalDate lastTradingDay() {
        LocalDate d = LocalDate.now(ET);
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY
                || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.minusDays(1);
        }
        return d;
    }

    /**
     * Steps backward exactly {@code n} trading days from {@code start},
     * skipping Saturday and Sunday.
     */
    private LocalDate stepBack(LocalDate start, int n) {
        LocalDate d = start;
        for (int i = 0; i < n; i++) {
            d = d.minusDays(1);
            while (d.getDayOfWeek() == DayOfWeek.SATURDAY
                    || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                d = d.minusDays(1);
            }
        }
        return d;
    }

    // ── Data fetch / cache ────────────────────────────────────────────────────

    private void fetchOrLoad(String tf) {
        String key = currentTicker + "-" + tf;

        // Cache hit → render immediately without a network call
        if (cacheClose.containsKey(key)) {
            double[] cls = cacheClose.get(key);
            long[] ts = cacheTs.get(key);
            prices.clear();
            timestamps.clear();
            for (int i = 0; i < cls.length; i++) {
                prices.add(cls[i]);
                timestamps.add(ts[i]);
            }
            setStatus("");
            computeLayout();
            repaint();
            notifyListener();
            return;
        }

        paintEmpty("Loading " + tf + " data…");
        setStatus("Fetching " + currentTicker + " " + tf + "…");

        // ── Polygon aggregates parameters ─────────────────────────────────────
        // from / to are "YYYY-MM-DD" date strings (Polygon uses inclusive date ranges).
        // lastTradingDay() resolves to the most recent Mon–Fri from today.
        LocalDate lastTD = lastTradingDay(); // e.g. 2026-03-27 (Friday)

        final String fromDate;
        final String toDate = lastTD.toString(); // always end at last trading day
        final int mult;
        final String span;
        final int limit;

        // minute changes 10m 20m, less calls = faster
        // Changed multi to 5+ or 15+ for less rate limit of polygon, less bars =
        // better.
        switch (tf) {
            case "1D" -> {
                // To get 200 prior 5-min bars, fetch ~4 days instead of 1
                fromDate = stepBack(lastTD, 4).toString();
                mult = 10;
                span = "minute";
                limit = 5000;
            }
            case "5D" -> {
                // To get 200 prior 15-min bars, fetch ~14 days instead of 5
                fromDate = stepBack(lastTD, 14).toString();
                mult = 20;
                span = "minute";
                limit = 5000;
            }
            case "1M" -> {
                // 1 Month chart (~21 days). Pad with 200 trading days (~10 months)
                fromDate = lastTD.minusMonths(11).toString();
                mult = 1;
                span = "day";
                limit = 5000;
            }
            case "3M" -> {
                // 3 Month chart (~63 days). Pad to 13 months.
                fromDate = lastTD.minusMonths(13).toString();
                mult = 1;
                span = "day";
                limit = 5000;
            }
            case "6M" -> {
                // 6 Month chart (~125 days). Pad to 16 months.
                fromDate = lastTD.minusMonths(16).toString();
                mult = 1;
                span = "day";
                limit = 5000;
            }
            case "1Y" -> {
                // 1 Year chart (~252 days). Pad to 22 months.
                fromDate = lastTD.minusMonths(22).toString();
                mult = 1;
                span = "day";
                limit = 5000;
            }
            default -> { // 2Y
                // 2 Year chart (~504 days). Pad to 36 months.
                fromDate = lastTD.minusMonths(36).toString();
                mult = 1;
                span = "day";
                limit = 5000;
            }
        }

        final String ticker = currentTicker;
        final String timeframe = tf;
        final int m = mult;
        final String s = span;
        final int l = limit;

        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() {
                return polygon.getAggregates(ticker, m, s, fromDate, toDate, l);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setStatus("");
            parseAndStore(task.getValue(), timeframe, key);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            setStatus("Chart data unavailable");
            paintEmpty("No data available");
        }));

        new Thread(task, "polygon-chart").start();
    }

    // ── Data parsing ──────────────────────────────────────────────────────────

    private void parseAndStore(JsonObject json, String tf, String cacheKey) {
        prices.clear();
        timestamps.clear();

        if (json == null) {
            paintEmpty("Network error – check connection");
            return;
        }

        // Polygon returns "status": "OK" on success; "FORBIDDEN" / "ERROR" etc.
        // otherwise
        String status = json.has("status") ? json.get("status").getAsString() : "UNKNOWN";
        if (!"OK".equalsIgnoreCase(status) && !"DELAYED".equalsIgnoreCase(status)) {
            System.out.println("Polygon status for " + cacheKey + ": " + status);
            paintEmpty(status.equals("FORBIDDEN")
                    ? "Polygon plan does not include intraday data"
                    : "No data available for " + tf);
            return;
        }

        if (!json.has("results") || json.get("results").isJsonNull()) {
            paintEmpty("No data available for " + tf);
            return;
        }

        JsonArray bars = json.getAsJsonArray("results");

        List<Double> fetchedPrices = new ArrayList<>();
        List<Long> fetchedTs = new ArrayList<>();

        for (var elem : bars) {
            JsonObject bar = elem.getAsJsonObject();
            double c = bar.has("c") ? bar.get("c").getAsDouble() : 0;
            long ts = bar.has("t") ? bar.get("t").getAsLong() : 0L;
            if (c <= 0 || ts <= 0)
                continue;

            // Strict time-filtering logic for minute bars
            if (tf.equals("1D") || tf.equals("5D")) {
                ZonedDateTime zdt = Instant.ofEpochMilli(ts).atZone(ET);
                int mins = zdt.getHour() * 60 + zdt.getMinute();
                if (mins < 9 * 60 + 30 || mins > 16 * 60)
                    continue;
            }

            fetchedPrices.add(c);
            fetchedTs.add(ts);
        }

        if (fetchedPrices.isEmpty()) {
            paintEmpty("No data available for " + tf);
            return;
        }

        // We fetched heavily padded data. Substring the exact visible boundaries.
        int plotBars = switch (tf) {
            case "1M" -> 21;
            case "3M" -> 63;
            case "6M" -> 126;
            case "1Y" -> 252;
            case "2Y" -> 504;
            case "1D" -> {
                if (fetchedTs.isEmpty())
                    yield 0;
                LocalDate lastDay = Instant.ofEpochMilli(fetchedTs.get(fetchedTs.size() - 1)).atZone(ET).toLocalDate();
                int count = 0;
                for (int i = fetchedTs.size() - 1; i >= 0; i--) {
                    if (Instant.ofEpochMilli(fetchedTs.get(i)).atZone(ET).toLocalDate().equals(lastDay)) {
                        count++;
                    } else {
                        break;
                    }
                }
                yield count;
            }
            case "5D" -> 130; // exactly 5 days of 15-minute bars (5 * 26)
            default -> fetchedPrices.size();
        };

        plotBars = Math.min(plotBars, fetchedPrices.size());
        int startIndex = fetchedPrices.size() - plotBars;

        for (int i = startIndex; i < fetchedPrices.size(); i++) {
            prices.add(fetchedPrices.get(i));
            timestamps.add(fetchedTs.get(i));
        }

        System.out.printf("Polygon %s %s: Plotting %d bars (Total fetched: %d)%n", currentTicker, tf, prices.size(),
                fetchedPrices.size());

        // ── Cache the result ──────────────────────────────────────────────────
        cacheClose.put(cacheKey, prices.stream().mapToDouble(Double::doubleValue).toArray());
        cacheTs.put(cacheKey, timestamps.stream().mapToLong(Long::longValue).toArray());
        cacheFullClose.put(cacheKey, fetchedPrices.stream().mapToDouble(Double::doubleValue).toArray());

        computeLayout();
        repaint();
        notifyListener();
    }

    // ── Layout computation ────────────────────────────────────────────────────

    private void computeLayout() {
        int n = prices.size();
        if (n == 0)
            return;

        double lo = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double hi = prices.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double span = hi - lo;
        if (span == 0)
            span = Math.max(lo * 0.01, 0.01);

        double pad = span * 0.08; // 8% breathing room top and bottom
        axisLo = lo - pad;
        axisHi = hi + pad;
        double axisSpan = axisHi - axisLo;

        lineColor = (prices.get(n - 1) >= prices.get(0)) ? UP : DOWN;

        px = new double[n];
        py = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = PL + (n == 1 ? PLOT_W / 2.0 : (i / (double) (n - 1)) * PLOT_W);
            py[i] = PT + PLOT_H - ((prices.get(i) - axisLo) / axisSpan) * PLOT_H;
        }
    }

    // ── Canvas rendering ──────────────────────────────────────────────────────

    private void repaint() {
        if (prices.isEmpty()) {
            paintEmpty("");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        int n = prices.size();

        gc.setFill(BG);
        gc.fillRect(0, 0, CW, CH);

        paintYAxis(gc);

        // Gradient area fill beneath the price line
        LinearGradient grad = new LinearGradient(
                0, PT, 0, PT + PLOT_H, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, lineColor.deriveColor(0, 1, 1, 0.40)),
                new Stop(1.0, lineColor.deriveColor(0, 1, 1, 0.00)));
        gc.setFill(grad);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++)
            gc.lineTo(px[i], py[i]);
        gc.lineTo(px[n - 1], PT + PLOT_H);
        gc.lineTo(px[0], PT + PLOT_H);
        gc.closePath();
        gc.fill();

        // Price line
        gc.setStroke(lineColor);
        gc.setLineWidth(1.8);
        gc.setLineDashes(0);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++)
            gc.lineTo(px[i], py[i]);
        gc.stroke();

        // Bottom border line
        gc.setStroke(GRID);
        gc.setLineWidth(0.8);
        gc.strokeLine(PL, PT + PLOT_H, PL + PLOT_W, PT + PLOT_H);

        paintXAxis(gc);

        if (hoverIdx >= 0 && hoverIdx < n)
            paintHover(gc, hoverIdx);
        else
            paintEndDot(gc, n - 1);
    }

    // ── Y-axis ────────────────────────────────────────────────────────────────

    private void paintYAxis(GraphicsContext gc) {
        if (axisHi <= axisLo)
            return;
        double[] ticks = niceYTicks(axisLo, axisHi, 5);
        double span = axisHi - axisLo;

        gc.setFont(Font.font("Arial", 10));
        gc.setTextAlign(TextAlignment.RIGHT);

        for (double tick : ticks) {
            if (tick < axisLo - 1e-9 || tick > axisHi + 1e-9)
                continue;
            double y = PT + PLOT_H - ((tick - axisLo) / span) * PLOT_H;

            gc.setStroke(GRID);
            gc.setLineWidth(0.5);
            gc.setLineDashes(0);
            gc.strokeLine(PL, y, PL + PLOT_W, y);

            gc.setFill(AXIS_TXT);
            gc.fillText(formatPrice(tick), PL - 6, y + 4);
        }
    }

    // ── X-axis ────────────────────────────────────────────────────────────────

    private void paintXAxis(GraphicsContext gc) {
        if (timestamps.isEmpty())
            return;
        gc.setFont(Font.font("Arial", 10));
        gc.setFill(AXIS_TXT);
        gc.setTextAlign(TextAlignment.CENTER);

        List<Integer> idxs = xLabelIndices();
        String prevLbl = null;

        for (int idx : idxs) {
            if (idx < 0 || idx >= px.length)
                continue;
            double x = px[idx];
            String lbl = formatXLabel(timestamps.get(idx));
            if (lbl.equals(prevLbl))
                continue;
            prevLbl = lbl;

            gc.setStroke(GRID);
            gc.setLineWidth(0.6);
            gc.strokeLine(x, PT + PLOT_H, x, PT + PLOT_H + 4);

            gc.setFill(AXIS_TXT);
            gc.fillText(lbl, x, PT + PLOT_H + 20);
        }
    }

    private List<Integer> xLabelIndices() {
        List<Integer> raw = new ArrayList<>();
        int n = timestamps.size();
        if (n == 0)
            return raw;

        switch (activeTimeframe) {
            case "1D" -> {
                // 10:00 AM, 12:00 PM, 2:00 PM, 4:00 PM
                int prevHr = -1;
                for (int i = 0; i < n; i++) {
                    int hr = toZDT(timestamps.get(i)).getHour();
                    if (hr != prevHr && (hr == 10 || hr == 12 || hr == 14 || hr == 16)) {
                        raw.add(i);
                        prevHr = hr;
                    }
                }
            }
            case "5D" -> {
                // Center the label near the middle of each trading day (~12:00 PM)
                LocalDate prev = null;
                for (int i = 0; i < n; i++) {
                    ZonedDateTime z = toZDT(timestamps.get(i));
                    LocalDate d = z.toLocalDate();
                    if (!d.equals(prev) && z.getHour() >= 12) {
                        raw.add(i);
                        prev = d;
                    }
                }
            }
            case "1M" -> {
                // One label roughly start of week
                LocalDate prevWeek = null;
                for (int i = 0; i < n; i++) {
                    LocalDate d = toZDT(timestamps.get(i)).toLocalDate();
                    LocalDate weekStart = d.with(
                            java.time.temporal.TemporalAdjusters
                                    .previousOrSame(DayOfWeek.MONDAY));
                    if (!weekStart.equals(prevWeek)) {
                        raw.add(i);
                        prevWeek = weekStart;
                    }
                }
            }
            case "3M", "6M" -> {
                // Try to center label in the middle of each month (~15th)
                Month prevMo = null;
                for (int i = 0; i < n; i++) {
                    ZonedDateTime z = toZDT(timestamps.get(i));
                    Month mo = z.getMonth();
                    if (!mo.equals(prevMo) && z.getDayOfMonth() >= 15) {
                        raw.add(i);
                        prevMo = mo;
                    }
                }
                // Fallback for current month if we haven't hit the 15th
                if (!raw.isEmpty() && toZDT(timestamps.get(n - 1)).getMonth() != prevMo) {
                    raw.add(n - 1);
                }
            }
            default -> {
                if ("2Y".equals(activeTimeframe)) {
                    Month prevMo = null;
                    int cnt = 0;
                    for (int i = 0; i < n; i++) {
                        java.time.ZonedDateTime z = toZDT(timestamps.get(i));
                        Month mo = z.getMonth();
                        if (!mo.equals(prevMo) && z.getDayOfMonth() >= 15) {
                            if (cnt % 4 == 0) // Every roughly 4 months to prevent extreme overlapping before thin-out
                                raw.add(i);
                            cnt++;
                            prevMo = mo;
                        }
                    }
                } else {
                    // 1Y: every other month, mid-month
                    Month prevMo = null;
                    int cnt = 0;
                    for (int i = 0; i < n; i++) {
                        java.time.ZonedDateTime z = toZDT(timestamps.get(i));
                        Month mo = z.getMonth();
                        if (!mo.equals(prevMo) && z.getDayOfMonth() >= 15) {
                            if (cnt % 2 == 0)
                                raw.add(i);
                            cnt++;
                            prevMo = mo;
                        }
                    }
                }
            }
        }

        // Thin out labels that are closer than 55 px to prevent overlap
        List<Integer> filtered = new ArrayList<>();
        double lastX = -9999;
        for (int idx : raw) {
            if (idx < px.length && px[idx] - lastX >= 55) {
                filtered.add(idx);
                lastX = px[idx];
            }
        }
        return filtered;
    }

    private String formatXLabel(long epochMs) {
        ZonedDateTime z = toZDT(epochMs);
        return switch (activeTimeframe) {
            case "1D" -> z.format(DateTimeFormatter.ofPattern("h:mm a")); // "10:00 AM"
            case "5D" -> z.format(DateTimeFormatter.ofPattern("MMM d")); // "Mar 24"
            case "1M" -> z.format(DateTimeFormatter.ofPattern("MMM d")); // "Mar 10"
            case "3M", "6M" -> z.format(DateTimeFormatter.ofPattern("MMM")); // "Oct"
            default -> z.format(DateTimeFormatter.ofPattern("MMM ''yy")); // "May '25"
        };
    }

    // ── Hover overlay ─────────────────────────────────────────────────────────

    private void paintHover(GraphicsContext gc, int idx) {
        double x = px[idx];
        double y = py[idx];

        // Dashed vertical crosshair
        gc.setStroke(XHAIR);
        gc.setLineWidth(1.0);
        gc.setLineDashes(5, 4);
        gc.strokeLine(x, PT, x, PT + PLOT_H);
        gc.setLineDashes(0);

        // Dot on the price line
        gc.setFill(Color.WHITE);
        gc.fillOval(x - 5, y - 5, 10, 10);
        gc.setFill(lineColor);
        gc.fillOval(x - 3.5, y - 3.5, 7, 7);

        // Tooltip bubble
        String priceLine = String.format("$%.2f USD", prices.get(idx));
        String dateLine = formatTooltipDate(timestamps.get(idx));
        double tw = Math.max(priceLine.length(), dateLine.length()) * 7.4 + 20;
        double th = 44;

        double bx = x + 10;
        double by = y - th - 10;
        if (bx + tw > PL + PLOT_W)
            bx = x - tw - 10;
        if (by < PT)
            by = PT + 4;

        gc.setFill(TIP_BG);
        gc.fillRoundRect(bx, by, tw, th, 6, 6);
        gc.setStroke(GRID);
        gc.setLineWidth(0.6);
        gc.strokeRoundRect(bx, by, tw, th, 6, 6);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 11));
        gc.fillText(priceLine, bx + 8, by + 17);
        gc.setFill(AXIS_TXT);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText(dateLine, bx + 8, by + 34);
    }

    private String formatTooltipDate(long epochMs) {
        ZonedDateTime z = toZDT(epochMs);
        return switch (activeTimeframe) {
            case "1D", "5D" -> z.format(DateTimeFormatter.ofPattern("EEE, MMM d  h:mm a"));
            case "1M" -> z.format(DateTimeFormatter.ofPattern("EEE, MMM d"));
            default -> z.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        };
    }

    private void paintEndDot(GraphicsContext gc, int idx) {
        double x = px[idx], y = py[idx];
        gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9));
        gc.fillOval(x - 4.5, y - 4.5, 9, 9);
        gc.setFill(lineColor);
        gc.fillOval(x - 3, y - 3, 6, 6);
    }

    // ── Empty / loading / error state ─────────────────────────────────────────

    private void paintEmpty(String msg) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BG);
        gc.fillRect(0, 0, CW, CH);

        gc.setStroke(GRID);
        gc.setLineWidth(0.5);
        for (int i = 1; i <= 4; i++) {
            double y = PT + (i / 5.0) * PLOT_H;
            gc.strokeLine(PL, y, PL + PLOT_W, y);
        }
        gc.strokeLine(PL, PT + PLOT_H, PL + PLOT_W, PT + PLOT_H);

        if (msg != null && !msg.isEmpty()) {
            gc.setFill(AXIS_TXT);
            gc.setFont(Font.font("Arial", 12));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(msg, PL + 12, PT + PLOT_H / 2.0 + 5);
        }
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private int nearestIndex(double mouseX) {
        if (px == null || px.length == 0)
            return -1;
        int best = 0;
        double bestD = Math.abs(px[0] - mouseX);
        for (int i = 1; i < px.length; i++) {
            double d = Math.abs(px[i] - mouseX);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private ZonedDateTime toZDT(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ET);
    }

    private void setStatus(String msg) {
        statusLbl.setText(msg);
        statusLbl.setVisible(!msg.isEmpty());
    }

    /**
     * "Nice number" Y-axis tick algorithm — rounds the tick interval to a
     * human-readable power-of-10 multiple (0.01, 0.02, 0.05, 0.1, 0.25, 0.5, 1, 2,
     * 5, …).
     */
    private double[] niceYTicks(double lo, double hi, int desiredCount) {
        double range = hi - lo;
        if (range <= 0)
            return new double[] { lo };
        double rough = range / (desiredCount - 1.0);
        double mag = Math.pow(10, Math.floor(Math.log10(rough)));
        double[] candidates = { 1, 2, 2.5, 5, 10 };
        double step = mag;
        for (double c : candidates) {
            if (c * mag >= rough - 1e-9) {
                step = c * mag;
                break;
            }
        }
        double start = Math.ceil(lo / step) * step;
        List<Double> ticks = new ArrayList<>();
        for (double v = start; v <= hi + step * 1e-6; v += step) {
            ticks.add(v);
            if (ticks.size() > desiredCount + 2)
                break;
        }
        return ticks.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private String formatPrice(double p) {
        if (p >= 1000)
            return String.format("$%.0f", p);
        if (p >= 100)
            return String.format("$%.1f", p);
        if (p >= 10)
            return String.format("$%.2f", p);
        return String.format("$%.3f", p);
    }
}
