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
 * Thin HTTP client for the Polygon.io REST API v2.
 * Free tier: 5 requests per minute.
 *
 * <p>Implements simple retry-with-backoff on HTTP 429 (rate limited):
 * waits 15 seconds and retries once before giving up.
 */
public class PolygonClient {

    private static final String BASE_URL         = "https://api.polygon.io/";
    /** Minimum gap between consecutive requests (12 s ≈ 5 req/min) */
    private static final long   MIN_DELAY_MS     = 12_000;
    /** How long to pause when a 429 is received before retrying. */
    private static final long   RATE_LIMIT_WAIT  = 15_000;
    private static final int    MAX_RETRIES      = 2;

    private final String     apiKey;
    private final HttpClient httpClient;
    private final Gson       gson;
    private volatile long    lastRequestTime = 0;

    public PolygonClient(String apiKey) {
        this.apiKey     = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * GET a Polygon endpoint and return a parsed {@link JsonObject}.
     * The {@code ?apiKey=…} query parameter is appended automatically.
     *
     * @param endpoint path + optional query, e.g.
     *   {@code "v2/aggs/ticker/AAPL/range/5/minute/2025-01-01/2025-06-01?adjusted=true&sort=asc"}
     */
    public JsonObject get(String endpoint) {
        String raw = getRaw(endpoint);
        if (raw == null) return null;
        try {
            return gson.fromJson(raw, JsonObject.class);
        } catch (Exception e) {
            System.out.println("[Polygon] JSON parse error: " + e.getMessage());
            return null;
        }
    }

    /** GET and return the raw JSON string, with rate-limit retry logic. */
    public String getRaw(String endpoint) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            throttle();
            String sep = endpoint.contains("?") ? "&" : "?";
            String url  = BASE_URL + endpoint + sep + "apiKey=" + apiKey;

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                if (status == 200) return response.body();

                if (status == 429) {
                    System.out.printf("[Polygon] Rate limited (attempt %d/%d) – waiting %.0f s%n",
                            attempt + 1, MAX_RETRIES, RATE_LIMIT_WAIT / 1000.0);
                    try { Thread.sleep(RATE_LIMIT_WAIT); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue; // retry
                }

                // Non-retryable errors
                if (status == 401) System.out.println("[Polygon] UNAUTHORIZED – check API key");
                else if (status == 403) System.out.println("[Polygon] FORBIDDEN – may need paid plan");
                else System.out.println("[Polygon] HTTP " + status + ": " + response.body());
                return null;

            } catch (IOException e) {
                System.out.println("[Polygon] Connection error: " + e.getMessage());
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /** Enforces the minimum gap between consecutive API calls. */
    private synchronized void throttle() {
        long now     = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
