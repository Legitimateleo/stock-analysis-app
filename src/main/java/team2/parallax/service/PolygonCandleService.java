package team2.parallax.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import team2.parallax.api.PolygonClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service wrapping the Polygon.io Aggregates (Bars) endpoint for stock charts.
 *
 * <p><b>Timeframe → resolution mapping:</b>
 * <pre>
 *  1D  → 5-minute bars,  last completed trading day
 *  5D  → 1-hour bars,    last 5 trading days
 *  1M  → 1-day bars,     last calendar month
 *  6M  → 1-day bars,     last 6 calendar months
 *  1Y  → 1-week bars,    last 52 weeks
 * </pre>
 *
 * <p>Polygon timestamps are in <b>epoch-milliseconds</b>. Divide by 1 000 for epoch-seconds.
 *
 * <p>All dates are evaluated in US/Eastern (market) time.
 */
public class PolygonCandleService {

    /** US Eastern timezone – Polygon timestamps reference ET market hours. */
    private static final ZoneId          ET       = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PolygonClient client;

    public PolygonCandleService(PolygonClient client) {
        this.client = client;
    }

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Fetch OHLCV close-price bars for {@code ticker} covering the requested {@code period}.
     *
     * @param ticker symbol, e.g. "NVDA"
     * @param period one of "1D", "5D", "1M", "6M", "1Y"
     * @return parsed {@link CandleResult}, never null (may be empty on error)
     */
    public CandleResult getCandles(String ticker, String period) {
        // All date logic runs in ET so "today" matches the market calendar
        LocalDate today          = LocalDate.now(ET);
        LocalDate lastTradingDay = prevTradingDay(today);

        LocalDate from;
        LocalDate to;
        int    multiplier;
        String timespan;

        switch (period) {
            case "1D" -> {
                // 30-minute bars: gives ticks at 9:30, 10:00, 10:30 … 4:00
                from        = lastTradingDay;
                to          = lastTradingDay;
                multiplier  = 30;
                timespan    = "minute";
            }
            case "1M" -> {
                from        = lastTradingDay.minusMonths(1);
                to          = lastTradingDay;
                multiplier  = 1;
                timespan    = "day";
            }
            case "6M" -> {
                from        = lastTradingDay.minusMonths(6);
                to          = lastTradingDay;
                multiplier  = 1;
                timespan    = "day";
            }
            case "1Y" -> {
                from        = lastTradingDay.minusYears(1);
                to          = lastTradingDay;
                multiplier  = 1;
                timespan    = "week";
            }
            default -> {
                from        = lastTradingDay;
                to          = lastTradingDay;
                multiplier  = 5;
                timespan    = "minute";
            }
        }

        CandleResult result = fetch(ticker, multiplier, timespan, from, to);

        // For 1D: if the last trading day has no data (market holiday), fall back one day
        if (result.isEmpty() && "1D".equals(period)) {
            LocalDate prev = shiftBackTradingDays(lastTradingDay, 1);
            result = fetch(ticker, multiplier, timespan, prev, prev);
        }

        return result;
    }

    // =========================================================================
    //  PRIVATE – HTTP + parsing
    // =========================================================================

    private CandleResult fetch(String ticker, int mult, String span,
                                LocalDate from, LocalDate to) {
        String endpoint = String.format(
                "v2/aggs/ticker/%s/range/%d/%s/%s/%s?adjusted=true&sort=asc&limit=750",
                ticker, mult, span,
                from.format(DATE_FMT),
                to.format(DATE_FMT));

        System.out.printf("[Polygon] GET %s (%s..%s)%n", span, from, to);
        JsonObject response = client.get(endpoint);
        return parse(response);
    }

    private CandleResult parse(JsonObject resp) {
        CandleResult empty = new CandleResult(new long[0], new double[0]);
        if (resp == null) return empty;

        String status = resp.has("status") ? resp.get("status").getAsString() : "";
        // Polygon returns "OK", "DELAYED" (free plan = 15-min delay), or error strings
        if (!"OK".equalsIgnoreCase(status) && !"DELAYED".equalsIgnoreCase(status)) {
            System.out.println("[Polygon] status=" + status
                    + (resp.has("message") ? " – " + resp.get("message").getAsString() : ""));
            return empty;
        }

        if (!resp.has("results") || resp.get("results").isJsonNull()) return empty;
        JsonArray results = resp.getAsJsonArray("results");
        if (results == null || results.isEmpty()) return empty;

        int      n      = results.size();
        long[]   times  = new long[n];    // epoch-milliseconds
        double[] closes = new double[n];

        for (int i = 0; i < n; i++) {
            JsonObject bar = results.get(i).getAsJsonObject();
            times[i]  = bar.has("t") ? bar.get("t").getAsLong()    : 0L;
            closes[i] = bar.has("c") ? bar.get("c").getAsDouble()   : 0.0;
        }

        System.out.printf("[Polygon] Received %d bars%n", n);
        return new CandleResult(times, closes);
    }

    // =========================================================================
    //  PRIVATE – trading-day calendar helpers
    // =========================================================================

    /**
     * Returns the most recent completed trading day on or before {@code date}.
     * Skips back over weekends. (Public holidays are not modelled here – if a
     * holiday is encountered the data set will simply be empty and the caller
     * can fall back one more day.)
     */
    private static LocalDate prevTradingDay(LocalDate date) {
        LocalDate d = date;
        // If we're currently before market close (4 PM ET) use yesterday
        // For simplicity always use yesterday so we get completed sessions
        d = d.minusDays(1);
        while (isWeekend(d)) d = d.minusDays(1);
        return d;
    }

    /**
     * Shifts {@code from} back by exactly {@code days} trading days
     * (weekends not counted).
     */
    private static LocalDate shiftBackTradingDays(LocalDate from, int days) {
        LocalDate d = from;
        for (int i = 0; i < days; i++) {
            d = d.minusDays(1);
            while (isWeekend(d)) d = d.minusDays(1);
        }
        return d;
    }

    private static boolean isWeekend(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    // =========================================================================
    //  Inner result type
    // =========================================================================

    /**
     * Parsed candle result. {@code timestamps} are in <b>epoch-milliseconds</b>.
     * Divide by 1 000 to convert to epoch-seconds for Java time APIs.
     */
    public record CandleResult(long[] timestamps, double[] closes) {
        public boolean isEmpty() { return timestamps.length == 0; }
    }
}
