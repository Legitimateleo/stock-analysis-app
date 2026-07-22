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
 * FinnhubClient is the Data Access Object (DAO) responsible for all HTTP
 * communication with the Finnhub REST API. It implements the DataAccessClient
 * interface, ensuring the Service Layer never depends on this concrete class
 * directly.
 *
 * <p>All market data used by MarketDataService flows through this class,
 * including real-time quotes, financial metrics, company profiles, and
 * analyst recommendation trends.</p>
 *
 * <p>Rate limiting is enforced internally to comply with the Finnhub free
 * tier limit. The client also handles HTTP redirect following automatically
 * via the HttpClient configuration.</p>
 *
 * @see DataAccessClient
 * @see team2.parallax.service.MarketDataService
 */
public class FinnhubClient implements DataAccessClient{

    /**
     * The base URL for all Finnhub API v1 endpoints.
     * Individual endpoint paths are appended to this string at request time.
     */
    private static final String BASE_URL = "https://finnhub.io/api/v1/";
    /**
     * Minimum delay in milliseconds enforced between consecutive API requests.
     * Set to 35ms to stay within the Finnhub free tier burst limit of
     * approximately 30 requests per second.
     */
    private static final long MIN_DELAY_MS = 35;

    /**
     * The Finnhub API key used to authenticate all outgoing requests.
     * Appended as a query parameter ("token=") to every request URL.
     * Stored as a private final field to prevent external access or mutation.
     * Loaded from config.properties at application startup via MainWindow.init().
     */
    private final String apiKey;

    /**
     * The Java HttpClient instance used to send all HTTP requests.
     * Configured to automatically follow HTTP redirects, which is required
     * for some Finnhub endpoints that redirect before returning data.
     */
    private final HttpClient httpClient;
    /**
     * Gson instance used to parse raw JSON strings into JsonObject instances.
     * Configured with pretty printing for cleaner debug output when needed.
     */
    private final Gson gson;
    /**
     * Timestamp in milliseconds of the most recent API request.
     * Used by the rate limiter in getRaw() to calculate elapsed time
     * and enforce the MIN_DELAY_MS gap between requests.
     */
    private long lastRequestTime = 0;

    /**
     * Constructs a new FinnhubClient with the provided API key.
     * Initializes the HttpClient with redirect-following enabled and
     * creates a Gson parser instance for JSON deserialization.
     *
     * @param apiKey the Finnhub API authentication key loaded from
     *               config.properties. Must not be null or empty.
     */
    public FinnhubClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Sends an HTTP GET request to the specified Finnhub endpoint and returns
     * the response body parsed as a {@link JsonObject}.
     *
     * <p>This method is used for all Finnhub endpoints that return a JSON
     * object at the top level. Endpoints that return a JSON array at the top
     * level (such as /stock/recommendation) must use {@link #getRaw(String)}
     * instead, as Gson cannot parse a JSON array directly into a JsonObject.</p>
     *
     * @param endpoint the Finnhub API endpoint path and query parameters,
     *                 excluding the base URL and API token. For example:
     *                 {@code "quote?symbol=AAPL"} or
     *                 {@code "stock/metric?symbol=NVDA&metric=all"}.
     * @return a parsed {@link JsonObject} containing the API response data,
     *         or {@code null} if the request failed or the response could
     *         not be parsed as a JSON object.
     */
    @Override
    public JsonObject get(String endpoint){
        String raw = getRaw(endpoint);
        if (raw == null) return null;
        try{
            return gson.fromJson(raw, JsonObject.class);
        } catch (Exception e) {
            System.out.println("ERROR parsing JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends an HTTP GET request to the specified Finnhub endpoint and returns
     * the raw JSON response body as a String without any parsing.
     *
     * <p>This method is the core HTTP transport for FinnhubClient. All requests
     * pass through here, including those initiated by {@link #get(String)}.
     * It is also called directly by MarketDataService for endpoints that return
     * JSON arrays at the top level, such as /stock/recommendation, which cannot
     * be parsed directly into a JsonObject.</p>
     *
     * <p>Rate limiting is enforced here before every request. If the time
     * elapsed since the last request is less than {@code MIN_DELAY_MS}, the
     * calling thread is paused for the remaining delay. This ensures the client
     * stays within the Finnhub free tier request rate.</p>
     *
     * <p>The API key is appended to the URL as a query parameter named
     * {@code token}. If the endpoint already contains a {@code ?} character,
     * the token is appended with {@code &}; otherwise it is appended with
     * {@code ?} to form a valid query string.</p>
     *
     * @param endpoint the Finnhub API endpoint path and query parameters,
     *                 excluding the base URL and API token. For example:
     *                 {@code "stock/recommendation?symbol=AAPL"}.
     * @return the raw JSON response body as a String if the request succeeded
     *         with HTTP 200, or {@code null} if the request failed for any reason.
     */
    public String getRaw(String endpoint) {

        //rate limiting
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        //updates the counter.
        lastRequestTime = System.currentTimeMillis();

        //Build URL
        String url = BASE_URL + endpoint
                + (endpoint.contains("?") ? "&" : "?")
                + "token=" + apiKey;


        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            //gets the reason why there is an error
            //checks for 3 different status codes. then responds with a message catered to the status error recieved.
            int status = response.statusCode();
            if (status == 200) return response.body();
            else if (status == 429) System.out.println("RATE LIMITED -- wait and retry");
            else if (status == 401) System.out.println("UNAUTHORIZED -- check API key");
            else if (status == 403) System.out.println("FORBIDDEN -- may need paid plan");
            else System.out.println("HTTP ERROR " + status + ": " + response.body());

        } catch (IOException e) {
            System.out.println("Connection Error: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("REQUEST INTERRUPTED");
        }
        return null;
    }
}
