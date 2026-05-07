package team2.parallax.api;
import com.google.gson.JsonObject;

public interface ChartDataClient {


    /**
     * Fetch aggregate (OHLCV) bars for a given ticker and time range.
     *
     * @param ticker     e.g. "AAPL"
     * @param multiplier timespan multiplier (1, 5, ...)
     * @param timespan   "minute", "hour", "day", "week", "month"
     * @param from       date string "YYYY-MM-DD"
     * @param to         date string "YYYY-MM-DD"
     * @param limit      max bars (up to 50,000)
     * @return parsed JsonObject or null on error
     */
    JsonObject getAggregates(String ticker, int multiplier, String timespan,
                             String from, String to, int limit);

    boolean hasKey();
}

