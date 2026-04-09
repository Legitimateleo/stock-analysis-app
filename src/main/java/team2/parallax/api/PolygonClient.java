package team2.parallax.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Lightweight client for the Polygon.io REST API.
 * Used exclusively for fetching historical aggregate bars (OHLCV).
 *
 * Free-tier note: data is delayed 15 minutes and limited to
 * end-of-day granularity for most endpoints, but the aggregates
 * endpoint works well for 1D–1Y chart ranges on the free plan.
 */
public class PolygonClient {

    private static final String BASE_URL = "https://api.polygon.io";
    /**
     * Minimum delay between API calls. Polygon free tier = 5 req/min; 500 ms feels
     * responsive for tab-switching while staying well inside paid-tier limits.
     */
    private static final long MIN_DELAY_MS = 500;

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private long lastRequestTime = 0;

    public PolygonClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Fetch aggregate (OHLCV) bars.
     *
     * @param ticker     e.g. "AAPL"
     * @param multiplier timespan multiplier (1, 5, …)
     * @param timespan   "minute", "hour", "day", "week", "month"
     * @param from       date string "YYYY-MM-DD"
     * @param to         date string "YYYY-MM-DD"
     * @param limit      max bars (up to 50 000)
     * @return parsed JsonObject or null on error
     */
    public JsonObject getAggregates(String ticker, int multiplier, String timespan,
            String from, String to, int limit) {
        // Rate limit
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();

        String url = String.format(
                "%s/v2/aggs/ticker/%s/range/%d/%s/%s/%s?adjusted=true&sort=desc&limit=%d&apiKey=%s",
                BASE_URL, ticker, multiplier, timespan, from, to, limit, apiKey);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 200) {
                JsonObject obj = gson.fromJson(response.body(), JsonObject.class);
                if (obj.has("results") && obj.get("results").isJsonArray()) {
                    com.google.gson.JsonArray arr = obj.getAsJsonArray("results");
                    com.google.gson.JsonArray rev = new com.google.gson.JsonArray();
                    for (int i = arr.size() - 1; i >= 0; i--) rev.add(arr.get(i));
                    obj.add("results", rev);
                }
                return obj;
            } else if (status == 429) {
                System.out.println("POLYGON RATE LIMITED");
            } else if (status == 403) {
                System.out.println("POLYGON FORBIDDEN – check API key / plan");
            } else {
                System.out.println("POLYGON HTTP " + status + ": " + response.body());
            }
        } catch (IOException e) {
            System.out.println("POLYGON Connection Error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Fetch quarterly financial data (income statement) from Polygon's
     * experimental financials endpoint.
     *
     * @param ticker e.g. "AAPL"
     * @return parsed JsonObject with "results" array, or null on error
     */
    public JsonObject getFinancials(String ticker) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();

        String url = String.format(
                "%s/vX/reference/financials?ticker=%s&timeframe=quarterly&limit=6&sort=period_of_report_date&order=desc&apiKey=%s",
                BASE_URL, ticker, apiKey);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 200) {
                return gson.fromJson(response.body(), JsonObject.class);
            } else if (status == 403) {
                System.out.println("POLYGON FINANCIALS FORBIDDEN – requires Stocks Advanced plan");
            } else if (status == 429) {
                System.out.println("POLYGON FINANCIALS RATE LIMITED");
            } else {
                System.out.println("POLYGON FINANCIALS HTTP " + status + ": " + response.body());
            }
        } catch (IOException e) {
            System.out.println("POLYGON FINANCIALS Connection Error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
