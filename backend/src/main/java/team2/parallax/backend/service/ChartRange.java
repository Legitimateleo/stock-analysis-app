package team2.parallax.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Translates the range selector the frontend exposes (1D/1W/1M/3M/1Y/5Y)
 * into the (multiplier, timespan, from, to) parameters Polygon's
 * aggregates endpoint expects.
 *
 * <p>Bucket sizes widen as the range grows so each request stays well
 * under Polygon's free-tier 50,000-bar cap and renders a readable chart
 * (e.g. 5 years of daily bars would be ~1,250 points — a monthly bucket
 * keeps that at ~60).</p>
 */
public enum ChartRange {
    ONE_DAY("1D", 5, "minute", 1),
    ONE_WEEK("1W", 1, "day", 7),
    ONE_MONTH("1M", 1, "day", 30),
    THREE_MONTH("3M", 1, "day", 90),
    ONE_YEAR("1Y", 1, "week", 365),
    FIVE_YEAR("5Y", 1, "month", 1825);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Map<String, ChartRange> BY_CODE = Map.of(
            "1D", ONE_DAY, "1W", ONE_WEEK, "1M", ONE_MONTH,
            "3M", THREE_MONTH, "1Y", ONE_YEAR, "5Y", FIVE_YEAR
    );

    public final String code;
    public final int multiplier;
    public final String timespan;
    private final int lookbackDays;

    ChartRange(String code, int multiplier, String timespan, int lookbackDays) {
        this.code = code;
        this.multiplier = multiplier;
        this.timespan = timespan;
        this.lookbackDays = lookbackDays;
    }

    /** Case-insensitive lookup; returns null if the code isn't recognized. */
    public static ChartRange fromCode(String code) {
        if (code == null) return null;
        return BY_CODE.get(code.trim().toUpperCase());
    }

    public String fromDate() {
        return LocalDate.now().minusDays(lookbackDays).format(FMT);
    }

    public String toDate() {
        return LocalDate.now().format(FMT);
    }
}
