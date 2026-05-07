package team2.parallax.service;
import team2.parallax.api.DataAccessClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.data.Fortune500;
import team2.parallax.model.StockSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;

/**
 * MarketDataService is the Facade of the Parallax system. It implements
 * the {@link MarketDataProvider} interface and acts as the single point
 * of entry for all market data operations requested by the UI layer.
 *
 * <p>This class hides the complexity of communicating with the Finnhub API,
 * parsing raw JSON responses, coordinating multiple endpoint calls, and
 * assembling domain model objects. The UI layer — specifically
 * {@code ParallaxController} — interacts exclusively with the five public
 * methods defined in {@link MarketDataProvider}, never with the underlying
 * {@link DataAccessClient} or JSON structures directly.</p>
 *
 * <p>The Facade boundary is enforced by the fact that no {@code JsonObject}
 * or {@code JsonArray} ever crosses out of this class. All raw API responses
 * are parsed internally and returned to callers as clean Java objects:
 * {@link Fortune500}, {@link StockSnapshot}, and
 * {@link RecommendationTrends}.</p>
 *
 * <p>This class depends on {@link DataAccessClient} rather than
 * {@code FinnhubClient} directly, ensuring the data source can be swapped
 * without modifying any service or UI logic.</p>
 *
 * @see MarketDataProvider
 * @see team2.parallax.api.DataAccessClient
 * @see team2.parallax.ui.ParallaxController
 */

public class MarketDataService implements MarketDataProvider {
    /**
     * The data access client used for all Finnhub API communication.
     * Declared as {@link DataAccessClient} interface rather than the
     * concrete {@code FinnhubClient} to enforce the DAO layer boundary.
     */
    private final DataAccessClient client;
    /**
     * The data access client used for all Finnhub API communication.
     * Declared as {@link DataAccessClient} interface rather than the
     * concrete {@code FinnhubClient} to enforce the DAO layer boundary.
     */
    private final Gson gson = new Gson();
    /**
     * Constructs a new MarketDataService with the provided data access client.
     *
     * @param client the {@link DataAccessClient} implementation to use for
     *               all Finnhub API requests. Typically a {@code FinnhubClient}
     *               instance wired in {@code MainWindow.init()}.
     */
    public MarketDataService(DataAccessClient client) {
        this.client = client;
    }

    /**
     * Fetches the raw analyst recommendation trends JSON array from the
     * Finnhub {@code /stock/recommendation} endpoint for the given ticker.
     *
     * <p>This method is called internally by {@link #getTrends(Fortune500)}
     * and returns the unparsed {@code JsonArray} for further processing.
     * It uses {@code getRaw()} rather than {@code get()} because Finnhub
     * returns a JSON array at the top level for this endpoint, which cannot
     * be parsed directly into a {@code JsonObject}.</p>
     *
     * @param symbol the stock ticker symbol to fetch recommendations for
     *               (e.g. {@code "AAPL"}).
     * @return a {@link JsonArray} containing monthly recommendation objects,
     *         or {@code null} if the request failed or returned no data.
     */
    public JsonArray getRecommendationTrends(String symbol) {
        String raw = client.getRaw("stock/recommendation?symbol=" + symbol);
        if (raw == null) return null;
        return gson.fromJson(raw, JsonArray.class);
    }

    /**
     * Searches for a Fortune 500 stock matching the given user input.
     * Performs two passes through the {@link Fortune500} enum:
     *
     * <ol>
     *   <li><b>Exact ticker match</b> — normalizes the input to uppercase
     *       and compares against each enum constant's name (ticker symbol).
     *       This pass is prioritized so that ticker searches always take
     *       precedence over name searches.</li>
     *   <li><b>Partial company name match</b> — if no ticker match is found,
     *       checks whether any company name contains the normalized input
     *       as a substring. This allows natural language searches such as
     *       "nvidia" or "apple" to resolve correctly.</li>
     * </ol>
     *
     * <p>No API call is made during this method. All validation is performed
     * entirely against the local {@link Fortune500} enum, which is a key
     * performance and architectural decision.</p>
     *
     * @param input the raw user input from the search field. May be a ticker
     *              symbol (e.g. {@code "NVDA"}) or a company name fragment
     *              (e.g. {@code "nvidia"}). Leading/trailing whitespace is
     *              trimmed before comparison.
     * @return the matching {@link Fortune500} enum constant, or {@code null}
     *         if no match is found in either pass.
     */
    @Override
    public Fortune500 search(String input) {
        String normalized = input.trim().toUpperCase();

        // Pass 1 — exact ticker match (case-insensitive via toUpperCase normalization)
        for (Fortune500 stock : Fortune500.values()) {
            if (stock.name().equals(normalized)) {
                return stock;
            }
        }
        // Pass 2 — partial company name match
        for (Fortune500 stock : Fortune500.values()) {
            if (stock.getCompanyName().toUpperCase().contains(normalized)) {
                return stock;
            }
        }

        return null;
    }

    /**
     * Returns a list of all Fortune 500 companies in the same industry
     * as the given stock, excluding the stock itself.
     *
     * <p>This method performs a full iteration of the {@link Fortune500} enum
     * and compares each entry's industry string against the given stock's
     * industry. No API call is made — related stock discovery is entirely
     * local, making this operation instantaneous regardless of network
     * conditions.</p>
     *
     * <p>The returned list is used to populate the Related Stocks panel
     * in {@code MainWindow} as clickable ticker chips that trigger a new
     * search when clicked.</p>
     *
     * @param stock the {@link Fortune500} stock whose industry peers
     *              should be returned. This stock is excluded from the
     *              returned list.
     * @return a {@link List} of {@link Fortune500} constants sharing the
     *         same industry classification as {@code stock}, never null
     *         but may be empty if no peers are found.
     */
    @Override
    public List<Fortune500> getByIndustry(Fortune500 stock) {
        List<Fortune500> related = new ArrayList<>();
        for (Fortune500 s : Fortune500.values()) {
            if (s != stock && s.getIndustry().equals(stock.getIndustry())) {
                related.add(s);
            }
        }
        return related;
    }

    /**
     * Fetches and returns the analyst recommendation trends for the given
     * stock from the Finnhub {@code /stock/recommendation} endpoint.
     *
     * <p>Internally calls {@link #getRecommendationTrends(String)} to
     * retrieve the raw JSON array, then iterates each element and constructs
     * a {@link RecommendationTrends} object per monthly entry. Missing JSON
     * fields are defaulted to {@code 0} or {@code "N/A"} rather than
     * throwing exceptions, ensuring the method degrades gracefully when
     * partial data is returned.</p>
     *
     * <p>This method makes exactly one Finnhub API call and is only triggered
     * when the user explicitly clicks the Show Trends button — it is never
     * called automatically as part of the initial stock search.</p>
     *
     * @param stock the {@link Fortune500} stock to retrieve recommendation
     *              trends for.
     * @return a {@link List} of {@link RecommendationTrends} objects, one per
     *         month of available data. Returns an empty list if the API call
     *         fails or returns no data — never returns {@code null}.
     */
    @Override
    public List<RecommendationTrends> getTrends(Fortune500 stock) {
        List<RecommendationTrends> trends = new ArrayList<>();
        JsonArray trendsData = getRecommendationTrends(stock.name());
        if (trendsData == null) return trends;

        for (JsonElement elem : trendsData) {
            JsonObject t = elem.getAsJsonObject();
            trends.add(new RecommendationTrends(
                    t.has("buy") ? t.get("buy").getAsInt() : 0,
                    t.has("hold") ? t.get("hold").getAsInt() : 0,
                    t.has("period") ? t.get("period").getAsString() : "N/A",
                    t.has("sell") ? t.get("sell").getAsInt() : 0,
                    t.has("strongBuy") ? t.get("strongBuy").getAsInt() : 0,
                    t.has("strongSell") ? t.get("strongSell").getAsInt() : 0
            ));
        }
        return trends;
    }

    /**
     * Fetches and assembles a complete {@link StockSnapshot} for the given
     * stock by making three sequential Finnhub API calls:
     *
     * <ol>
     *   <li>{@code /quote} — retrieves the real-time price, dollar change,
     *       and percentage change for the day.</li>
     *   <li>{@code /stock/metric} — retrieves financial metrics including
     *       P/E ratio, Price-to-Book, dividend yield, 52-week high/low,
     *       free cash flow per share, market cap, EPS, and revenue growth.</li>
     *   <li>{@code /stock/profile2} — retrieves the company logo URL via
     *       {@link #getLogoUrl(String)}.</li>
     * </ol>
     *
     * <p>Each API response is parsed defensively. If a response is null or
     * a specific field is missing, the corresponding value defaults to
     * {@code 0.0} via {@link #getDoubleOrZero(JsonObject, String)} and
     * {@link #getMetricValue(JsonObject, String)}, ensuring the snapshot
     * is always returned in a valid state.</p>
     *
     * <p>This method is the most API-intensive operation in the system,
     * consuming three of the available Finnhub free tier calls per search.
     * It is only triggered once per stock search — never called redundantly.</p>
     *
     * @param stock the {@link Fortune500} stock to fetch snapshot data for.
     *              The enum constant's {@code name()} is used as the ticker
     *              symbol in all three API requests.
     * @return a fully assembled {@link StockSnapshot} containing all available
     *         financial data for the given stock. Fields that could not be
     *         retrieved default to {@code 0.0} or {@code "N/A"}.
     */
    @Override
    public StockSnapshot getSnapshot(Fortune500 stock) {
        String symbol = stock.name();

        // Call 1 — /quote: real-time price, change, and percent change
        JsonObject quoteData = client.get("quote?symbol=" + symbol);
        double currentPrice = 0, change = 0, changePercent = 0;
        if (quoteData != null) {
            currentPrice = getDoubleOrZero(quoteData, "c");
            change = getDoubleOrZero(quoteData, "d");
            changePercent = getDoubleOrZero(quoteData, "dp");
        }
        // Call 2 — /stock/metric: financial ratios and performance indicators
        JsonObject metricsData = client.get("stock/metric?symbol=" + symbol + "&metric=all");
        double peRatio = 0, priceToBook = 0, dividendYield = 0,
                weekHigh52 = 0, weekLow52 = 0, freeCashFlowPerShare = 0;
        double marketCap = 0, eps = 0, revenueYoy = 0;
        if (metricsData != null) {
            JsonObject m = metricsData.getAsJsonObject("metric");
            if (m != null) {
                peRatio = getMetricValue(m, "peBasicExclExtraTTM");
                priceToBook = getMetricValue(m, "pbAnnual");
                dividendYield = getMetricValue(m, "currentDividendYieldTTM");
                weekHigh52 = getMetricValue(m, "52WeekHigh");
                weekLow52 = getMetricValue(m, "52WeekLow");
                freeCashFlowPerShare = getMetricValue(m, "cashFlowPerShareTTM");
                marketCap = getMetricValue(m, "marketCapitalization");
                eps = getMetricValue(m, "epsTTM");
                revenueYoy = getMetricValue(m, "revenueGrowthTTMYoy");
            }
        }

        //call 3 - logo for summary
        String logo = getLogoUrl(symbol);

        return new StockSnapshot(currentPrice, change, changePercent,
                peRatio, priceToBook, dividendYield,
                weekHigh52, weekLow52, freeCashFlowPerShare,
                marketCap, eps, revenueYoy, logo);
    }

    /**
     * Fetches and returns the company logo URL for the given ticker symbol
     * from the Finnhub {@code /stock/profile2} endpoint.
     *
     * <p>Called internally by {@link #getSnapshot(Fortune500)} to retrieve
     * the logo displayed in the Parallax results panel header. Also available
     * publicly for fetching logos of related stocks in the Related Stocks panel
     * without triggering a full snapshot fetch.</p>
     *
     * @param symbol the stock ticker symbol to fetch the logo for
     *               (e.g. {@code "AAPL"}).
     * @return the logo URL string if available, or {@code "N/A"} if the
     *         profile response is null, missing the logo field, or the
     *         logo field is JSON null.
     */
    public String getLogoUrl(String symbol) {
        JsonObject profileData = client.get("stock/profile2?symbol=" + symbol);
        if (profileData != null && profileData.has("logo")
                && !profileData.get("logo").isJsonNull()) {
            return profileData.get("logo").getAsString();
        }
        return "N/A";
    }

    /**
     * Safely extracts a {@code double} value from a {@link JsonObject} by key.
     * Returns {@code 0.0} if the key is absent or its value is JSON null,
     * preventing {@code NullPointerException} when Finnhub omits optional fields.
     *
     * @param obj the {@link JsonObject} to extract the value from.
     * @param key the JSON field name to look up.
     * @return the double value associated with {@code key}, or {@code 0.0}
     *         if the key is missing or null.
     */
    private double getDoubleOrZero(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0;
    }
    /**
     * Safely extracts a {@code double} metric value from the nested
     * {@code "metric"} {@link JsonObject} returned by the Finnhub
     * {@code /stock/metric} endpoint.
     * Returns {@code 0.0} if the key is absent or its value is JSON null.
     *
     * @param metrics the {@code "metric"} {@link JsonObject} extracted from
     *                the full metrics API response.
     * @param key     the Finnhub metric field name to look up
     *                (e.g. {@code "peBasicExclExtraTTM"}).
     * @return the double value associated with {@code key}, or {@code 0.0}
     *         if the key is missing or null.
     */
    private double getMetricValue(JsonObject metrics, String key) {
        if (metrics.has(key) && !metrics.get(key).isJsonNull()) {
            return metrics.get(key).getAsDouble();
        }
        return 0;
    }
}






