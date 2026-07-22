package team2.parallax.backend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import team2.parallax.api.ChartDataClient;
import team2.parallax.model.ChartPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloud-only service exposing historical OHLCV chart data for the
 * "change historical price graph range" use case (1D/1W/1M/3M/1Y/5Y).
 *
 * <p>This class is intentionally separate from {@code MarketDataService}.
 * Chart data was scoped as "desktop only" in the original Plan of Attack
 * (D4), so wiring Polygon into the shared facade — which the JavaFX app
 * also depends on via an unchanged constructor — would have forced a
 * breaking constructor change on both consumers. Keeping this as its own
 * service lets the backend expose chart data without touching
 * {@code MarketDataService} at all.</p>
 *
 * <p>Confirm D4 with the team before Track B (Streamlit) builds a chart
 * page against this — the Plan of Attack currently says this is out of
 * scope for the cloud deployment.</p>
 */
public class ChartDataService {

    private final ChartDataClient client;

    public ChartDataService(ChartDataClient client) {
        this.client = client;
    }

    /**
     * Fetches OHLCV bars for the given ticker over the given range.
     *
     * @param ticker stock ticker, e.g. "AAPL"
     * @param range  one of the {@link ChartRange} codes (1D/1W/1M/3M/1Y/5Y)
     * @return a list of {@link ChartPoint} bars, oldest first, or an empty
     *         list if the range code is invalid, the API key is missing,
     *         or the request fails.
     */
    public List<ChartPoint> getBars(String ticker, ChartRange range) {
        List<ChartPoint> points = new ArrayList<>();
        if (range == null || !client.hasKey()) {
            return points;
        }

        JsonObject response = client.getAggregates(
                ticker, range.multiplier, range.timespan,
                range.fromDate(), range.toDate(), 5000
        );
        if (response == null || !response.has("results") || !response.get("results").isJsonArray()) {
            return points;
        }

        JsonArray results = response.getAsJsonArray("results");
        for (JsonElement elem : results) {
            JsonObject bar = elem.getAsJsonObject();
            points.add(new ChartPoint(
                    bar.has("t") ? bar.get("t").getAsLong()   : 0L,
                    bar.has("o") ? bar.get("o").getAsDouble() : 0.0,
                    bar.has("h") ? bar.get("h").getAsDouble() : 0.0,
                    bar.has("l") ? bar.get("l").getAsDouble() : 0.0,
                    bar.has("c") ? bar.get("c").getAsDouble() : 0.0,
                    bar.has("v") ? bar.get("v").getAsLong()   : 0L
            ));
        }
        return points;
    }
}
