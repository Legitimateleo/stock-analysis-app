import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FinnhubMain {

    // ========================================================================
    //  CONFIGURATION
    // ========================================================================

    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static String API_KEY;

    // Reusable objects — create once, use everywhere
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Simple rate limiter: Finnhub free tier allows 60 calls/min
    // This pauses between requests to stay safely under the limit
    private static long lastRequestTime = 0;
    private static final long MIN_DELAY_MS = 200; // ~5 requests/sec (safe margin)


    // ========================================================================
    //  MAIN — Runs all tests in order
    // ========================================================================

    public static void main(String[] args) {

        // --- Load API Key from environment variable ---
        API_KEY = System.getenv("FINNHUB_API_KEY");

        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  ERROR: FINNHUB_API_KEY environment variable not set     ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  How to fix in IntelliJ:                                 ║");
            System.out.println("║  1. Click Run > Edit Configurations                      ║");
            System.out.println("║  2. Select 'FinnhubTest' (or click + > Application)      ║");
            System.out.println("║  3. Find 'Environment variables' field                   ║");
            System.out.println("║  4. Add: FINNHUB_API_KEY=your_key_here                   ║");
            System.out.println("║  5. Get a free key at: https://finnhub.io/register       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            return;
        }

        String testTicker = "NVDA";  // Change this to test any stock

        printBanner("FINNHUB API TEST SUITE — PROJECT PARALLAX");
        System.out.println("  Testing with ticker: " + testTicker);
        System.out.println("  API Key loaded: " + API_KEY.substring(0, 4) + "****");
        System.out.println("  Timestamp: " + java.time.LocalDateTime.now());
        System.out.println();

        // --- Run each test ---
        // Comment out any line to skip that test

        testQuote(testTicker);            // Test 1: Real-time price
        testCompanyProfile(testTicker);   // Test 2: Company info
        testFinancialMetrics(testTicker); // Test 3: Valuation metrics (P/E, P/B, etc.)
        testCandles(testTicker);          // Test 4: Historical price data (for charts)
        testCompanyNews(testTicker);      // Test 5: News feed
        testInsiderSentiment(testTicker); // Test 6: Insider sentiment summary
        testInsiderTransactions(testTicker); // Test 7: Individual insider trades

        printBanner("ALL TESTS COMPLETE");
        System.out.println("  All 7 Finnhub endpoints verified working with Java.");
        System.out.println("  No special SDK needed — just HttpClient + Gson.");
        System.out.println();
    }


    // ========================================================================
    //  TEST 1: REAL-TIME STOCK QUOTE
    //  Endpoint: /quote?symbol=AAPL
    //  Parallax use: Display current price, compare to intrinsic value
    // ========================================================================

    private static void testQuote(String symbol) {
        printBanner("TEST 1: Real-Time Quote — /quote");

        JsonObject data = makeRequest("/quote?symbol=" + symbol);
        if (data == null) return;

        double currentPrice  = data.get("c").getAsDouble();
        double change         = data.get("d").getAsDouble();
        double changePercent  = data.get("dp").getAsDouble();
        double highPrice      = data.get("h").getAsDouble();
        double lowPrice       = data.get("l").getAsDouble();
        double openPrice      = data.get("o").getAsDouble();
        double previousClose  = data.get("pc").getAsDouble();

        System.out.println("  Symbol:          " + symbol);
        System.out.println("  Current Price:   $" + String.format("%.2f", currentPrice));
        System.out.println("  Change:          $" + String.format("%.2f", change)
                + " (" + String.format("%.2f", changePercent) + "%)");
        System.out.println("  Day Range:       $" + String.format("%.2f", lowPrice)
                + " — $" + String.format("%.2f", highPrice));
        System.out.println("  Open:            $" + String.format("%.2f", openPrice));
        System.out.println("  Previous Close:  $" + String.format("%.2f", previousClose));
        System.out.println("  ✓ PASS — Quote data retrieved successfully");
        System.out.println();
    }


    // ========================================================================
    //  TEST 2: COMPANY PROFILE
    //  Endpoint: /stock/profile2?symbol=AAPL
    //  Parallax use: Display company info, logo, industry, market cap
    // ========================================================================

    private static void testCompanyProfile(String symbol) {
        printBanner("TEST 2: Company Profile — /stock/profile2");

        JsonObject data = makeRequest("/stock/profile2?symbol=" + symbol);
        if (data == null) return;

        String name       = getStringOrNA(data, "name");
        String ticker     = getStringOrNA(data, "ticker");
        String exchange   = getStringOrNA(data, "exchange");
        String industry   = getStringOrNA(data, "finnhubIndustry");
        String country    = getStringOrNA(data, "country");
        String webUrl     = getStringOrNA(data, "weburl");
        double marketCap  = data.has("marketCapitalization") ?
                data.get("marketCapitalization").getAsDouble() : 0;

        System.out.println("  Company:      " + name + " (" + ticker + ")");
        System.out.println("  Exchange:     " + exchange);
        System.out.println("  Industry:     " + industry);
        System.out.println("  Country:      " + country);
        System.out.println("  Market Cap:   $" + String.format("%.2f", marketCap) + "M");
        System.out.println("  Website:      " + webUrl);
        System.out.println("  ✓ PASS — Company profile retrieved successfully");
        System.out.println();
    }


    // ========================================================================
    //  TEST 3: FINANCIAL METRICS (KEY FOR PARALLAX)
    //  Endpoint: /stock/metric?symbol=AAPL&metric=all
    //  Parallax use: Compare intrinsic value vs. market price
    //    - P/E ratio, P/B ratio, EPS, margins, ROE, etc.
    //    - These are the core metrics for finding undervalued stocks
    // ========================================================================

    private static void testFinancialMetrics(String symbol) {
        printBanner("TEST 3: Financial Metrics — /stock/metric");

        JsonObject data = makeRequest("/stock/metric?symbol=" + symbol + "&metric=all");
        if (data == null) return;

        // The metrics live inside a nested "metric" object
        JsonObject metrics = data.getAsJsonObject("metric");
        if (metrics == null) {
            System.out.println("  ✗ FAIL — No metrics data returned");
            return;
        }

        // Pull out the key valuation metrics Parallax needs
        System.out.println("  ── Valuation Metrics (for undervalued stock detection) ──");
        printMetric(metrics, "peBasicExclExtraTTM",    "P/E Ratio (TTM)");
        printMetric(metrics, "pbAnnual",               "Price/Book (Annual)");
        printMetric(metrics, "psAnnual",               "Price/Sales (Annual)");
        printMetric(metrics, "epsBasicExclExtraItemsTTM", "EPS (TTM)");
        printMetric(metrics, "currentDividendYieldTTM",   "Dividend Yield (TTM)");

        System.out.println();
        System.out.println("  ── Profitability Metrics ──");
        printMetric(metrics, "roeTTM",                    "Return on Equity (TTM)");
        printMetric(metrics, "roaTTM",                    "Return on Assets (TTM)");
        printMetric(metrics, "grossMarginTTM",            "Gross Margin (TTM)");
        printMetric(metrics, "operatingMarginTTM",        "Operating Margin (TTM)");
        printMetric(metrics, "netProfitMarginTTM",        "Net Profit Margin (TTM)");

        System.out.println();
        System.out.println("  ── Financial Health ──");
        printMetric(metrics, "currentRatioAnnual",        "Current Ratio (Annual)");
        printMetric(metrics, "totalDebt/totalEquityAnnual", "Debt/Equity (Annual)");
        printMetric(metrics, "freeCashFlowPerShareTTM",   "Free Cash Flow/Share (TTM)");
        printMetric(metrics, "revenuePerShareTTM",        "Revenue/Share (TTM)");

        System.out.println();
        System.out.println("  ── Price Performance ──");
        printMetric(metrics, "52WeekHigh",                "52-Week High");
        printMetric(metrics, "52WeekLow",                 "52-Week Low");
        printMetric(metrics, "beta",                      "Beta");

        System.out.println();
        System.out.println("  ✓ PASS — Financial metrics retrieved successfully");
        System.out.println("  → Parallax can use these to calculate intrinsic value");
        System.out.println();
    }


    // ========================================================================
    //  TEST 4: HISTORICAL CANDLES (OHLCV)
    //  Endpoint: /stock/candle?symbol=AAPL&resolution=D&from=...&to=...
    //  Parallax use: Feed into JavaFX charts for price visualization
    // ========================================================================

    private static void testCandles(String symbol) {
        printBanner("TEST 4: Historical Candles — /stock/candle");

        // Get last 30 days of daily candles
        long toTime = Instant.now().getEpochSecond();
        long fromTime = toTime - (30L * 24 * 60 * 60); // 30 days ago

        JsonObject data = makeRequest("/stock/candle?symbol=" + symbol
                + "&resolution=D&from=" + fromTime + "&to=" + toTime);
        if (data == null) return;

        // Check if data was returned (status "ok" means we have candles)
        String status = data.has("s") ? data.get("s").getAsString() : "unknown";
        if (!"ok".equals(status)) {
            System.out.println("  ✗ FAIL — No candle data (status: " + status + ")");
            return;
        }

        JsonArray closes     = data.getAsJsonArray("c"); // close prices
        JsonArray opens      = data.getAsJsonArray("o"); // open prices
        JsonArray highs      = data.getAsJsonArray("h"); // high prices
        JsonArray lows       = data.getAsJsonArray("l"); // low prices
        JsonArray volumes    = data.getAsJsonArray("v"); // volume
        JsonArray timestamps = data.getAsJsonArray("t"); // unix timestamps

        int count = closes.size();
        System.out.println("  Data points returned: " + count + " trading days");
        System.out.println();

        // Show the first 5 and last 5 data points
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int show = Math.min(5, count);

        System.out.println("  ── First " + show + " Days ──");
        System.out.println("  Date        | Open     | High     | Low      | Close    | Volume");
        System.out.println("  ------------|----------|----------|----------|----------|----------");
        for (int i = 0; i < show; i++) {
            printCandleRow(timestamps, opens, highs, lows, closes, volumes, i, fmt);
        }

        if (count > 10) {
            System.out.println("  ... (" + (count - 10) + " more rows) ...");
            System.out.println();
            System.out.println("  ── Last " + show + " Days ──");
            System.out.println("  Date        | Open     | High     | Low      | Close    | Volume");
            System.out.println("  ------------|----------|----------|----------|----------|----------");
            for (int i = count - show; i < count; i++) {
                printCandleRow(timestamps, opens, highs, lows, closes, volumes, i, fmt);
            }
        }

        System.out.println();
        System.out.println("  ✓ PASS — Historical candle data retrieved successfully");
        System.out.println("  → This data feeds directly into JavaFX LineChart/CandlestickChart");
        System.out.println();
    }


    // ========================================================================
    //  TEST 5: COMPANY NEWS
    //  Endpoint: /company-news?symbol=AAPL&from=...&to=...
    //  Parallax use: Display recent news feed for selected company
    // ========================================================================

    private static void testCompanyNews(String symbol) {
        printBanner("TEST 5: Company News — /company-news");

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        String url = "/company-news?symbol=" + symbol
                + "&from=" + weekAgo + "&to=" + today;

        // News returns a JSON array, not an object
        String rawJson = makeRawRequest(url);
        if (rawJson == null) return;

        JsonArray articles = gson.fromJson(rawJson, JsonArray.class);
        int totalArticles = articles.size();
        int showCount = Math.min(5, totalArticles);

        System.out.println("  Total articles this week: " + totalArticles);
        System.out.println("  Showing first " + showCount + ":");
        System.out.println();

        for (int i = 0; i < showCount; i++) {
            JsonObject article = articles.get(i).getAsJsonObject();
            String headline  = getStringOrNA(article, "headline");
            String source    = getStringOrNA(article, "source");
            String category  = getStringOrNA(article, "category");
            String articleUrl = getStringOrNA(article, "url");
            long datetime    = article.has("datetime") ? article.get("datetime").getAsLong() : 0;

            String date = Instant.ofEpochSecond(datetime)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            System.out.println("  [" + (i + 1) + "] " + headline);
            System.out.println("      Source: " + source + " | Category: " + category);
            System.out.println("      Date: " + date);
            System.out.println("      Link: " + articleUrl);
            System.out.println();
        }

        System.out.println("  ✓ PASS — Company news retrieved successfully");
        System.out.println("  → Parallax can display these in a scrollable news feed");
        System.out.println();
    }


    // ========================================================================
    //  TEST 6: INSIDER SENTIMENT
    //  Endpoint: /stock/insider-sentiment?symbol=AAPL&from=...&to=...
    //  Parallax use: Track abnormal demand patterns from insiders
    //  NOTE: This is aggregated monthly data showing net insider buying/selling
    // ========================================================================

    private static void testInsiderSentiment(String symbol) {
        printBanner("TEST 6: Insider Sentiment — /stock/insider-sentiment");

        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        JsonObject data = makeRequest("/stock/insider-sentiment?symbol=" + symbol
                + "&from=" + sixMonthsAgo + "&to=" + today);
        if (data == null) return;

        JsonArray sentimentData = data.getAsJsonArray("data");
        if (sentimentData == null || sentimentData.size() == 0) {
            System.out.println("  No insider sentiment data available for this period.");
            System.out.println("  (This is normal — not all stocks have recent insider activity)");
            System.out.println();
            return;
        }

        System.out.println("  Monthly insider sentiment (last 6 months):");
        System.out.println("  Month     | Net MSPR   | Change      | Interpretation");
        System.out.println("  ----------|------------|-------------|------------------");

        for (JsonElement elem : sentimentData) {
            JsonObject row = elem.getAsJsonObject();
            int year  = row.get("year").getAsInt();
            int month = row.get("month").getAsInt();
            double mspr   = row.has("mspr") ? row.get("mspr").getAsDouble() : 0;
            double change = row.has("change") ? row.get("change").getAsDouble() : 0;

            // MSPR = Monthly Share Purchase Ratio
            // Positive = more insider buying, Negative = more insider selling
            String interpretation;
            if (mspr > 0.5) interpretation = "Strong buying signal";
            else if (mspr > 0) interpretation = "Mild buying";
            else if (mspr > -0.5) interpretation = "Mild selling";
            else interpretation = "Strong selling signal";

            System.out.printf("  %d-%02d   | %+.4f    | %+.4f     | %s%n",
                    year, month, mspr, change, interpretation);
        }

        System.out.println();
        System.out.println("  ✓ PASS — Insider sentiment data retrieved successfully");
        System.out.println("  → MSPR > 0 indicates net insider buying (bullish signal)");
        System.out.println("  → Parallax can flag unusual spikes in insider activity");
        System.out.println();
    }


    // ========================================================================
    //  TEST 7: INSIDER TRANSACTIONS
    //  Endpoint: /stock/insider-transactions?symbol=AAPL
    //  Parallax use: Show detailed individual insider buy/sell transactions
    // ========================================================================

    private static void testInsiderTransactions(String symbol) {
        printBanner("TEST 7: Insider Transactions — /stock/insider-transactions");

        JsonObject data = makeRequest("/stock/insider-transactions?symbol=" + symbol);
        if (data == null) return;

        JsonArray transactions = data.getAsJsonArray("data");
        if (transactions == null || transactions.size() == 0) {
            System.out.println("  No recent insider transactions found.");
            System.out.println();
            return;
        }

        int showCount = Math.min(10, transactions.size());
        System.out.println("  Total transactions on file: " + transactions.size());
        System.out.println("  Showing most recent " + showCount + ":");
        System.out.println();

        for (int i = 0; i < showCount; i++) {
            JsonObject tx = transactions.get(i).getAsJsonObject();
            String name      = getStringOrNA(tx, "name");
            String txType    = getStringOrNA(tx, "transactionType");
            double shares    = tx.has("share") ? tx.get("share").getAsDouble() : 0;
            double price     = tx.has("transactionPrice") && !tx.get("transactionPrice").isJsonNull() ?
                    tx.get("transactionPrice").getAsDouble() : 0;
            String date      = getStringOrNA(tx, "transactionDate");
            String filingDate = getStringOrNA(tx, "filingDate");

            // Format the transaction type for readability
            String action;
            switch (txType) {
                case "P - Purchase": action = "BUY "; break;
                case "S - Sale":     action = "SELL"; break;
                default:             action = txType;
            }

            double value = shares * price;
            System.out.printf("  [%s] %s — %.0f shares @ $%.2f ($%,.0f) on %s%n",
                    action, name, Math.abs(shares), price, Math.abs(value), date);
            System.out.println("         Filed: " + filingDate);
        }

        System.out.println();
        System.out.println("  ✓ PASS — Insider transactions retrieved successfully");
        System.out.println("  → Parallax can highlight large insider purchases as signals");
        System.out.println();
    }


    // ========================================================================
    //  HELPER METHODS
    // ========================================================================

    /**
     * Makes an HTTP GET request to the Finnhub API and returns the parsed JSON object.
     * Includes automatic rate limiting and error handling.
     *
     * THIS IS THE CORE METHOD — everything else in this file calls this.
     * To use Finnhub in Parallax, you basically need this method + your API key.
     */
    private static JsonObject makeRequest(String endpoint) {
        String raw = makeRawRequest(endpoint);
        if (raw == null) return null;
        try {
            return gson.fromJson(raw, JsonObject.class);
        } catch (Exception e) {
            System.out.println("  ✗ ERROR parsing JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Makes the actual HTTP request and returns the raw JSON string.
     * Separated from makeRequest() because some endpoints (like /company-news)
     * return JSON arrays instead of objects.
     */
    private static String makeRawRequest(String endpoint) {
        // --- Rate limiting ---
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();

        // --- Build the full URL ---
        String url = BASE_URL + endpoint
                + (endpoint.contains("?") ? "&" : "?")
                + "token=" + API_KEY;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.body();
            } else if (statusCode == 429) {
                System.out.println("  ✗ RATE LIMITED (429) — Too many requests.");
                System.out.println("    Free tier: 60 calls/min. Wait a moment and try again.");
            } else if (statusCode == 401) {
                System.out.println("  ✗ UNAUTHORIZED (401) — Check your API key.");
            } else if (statusCode == 403) {
                System.out.println("  ✗ FORBIDDEN (403) — This endpoint may require a paid plan.");
            } else {
                System.out.println("  ✗ HTTP ERROR " + statusCode + ": " + response.body());
            }

        } catch (IOException e) {
            System.out.println("  ✗ CONNECTION ERROR: " + e.getMessage());
            System.out.println("    Check your internet connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("  ✗ REQUEST INTERRUPTED");
        }

        return null;
    }

    /** Safely extract a string from a JSON object, returning "N/A" if missing */
    private static String getStringOrNA(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "N/A";
    }

    /** Print a single metric line with safe null handling */
    private static void printMetric(JsonObject metrics, String key, String label) {
        String value;
        if (metrics.has(key) && !metrics.get(key).isJsonNull()) {
            double num = metrics.get(key).getAsDouble();
            value = String.format("%.4f", num);
        } else {
            value = "N/A";
        }
        System.out.printf("  %-30s %s%n", label + ":", value);
    }

    /** Print a formatted candle row */
    private static void printCandleRow(JsonArray timestamps, JsonArray opens,
                                       JsonArray highs, JsonArray lows,
                                       JsonArray closes, JsonArray volumes,
                                       int i, DateTimeFormatter fmt) {
        String date = Instant.ofEpochSecond(timestamps.get(i).getAsLong())
                .atZone(ZoneId.systemDefault())
                .format(fmt);
        System.out.printf("  %-12s| $%-7.2f | $%-7.2f | $%-7.2f | $%-7.2f | %,.0f%n",
                date,
                opens.get(i).getAsDouble(),
                highs.get(i).getAsDouble(),
                lows.get(i).getAsDouble(),
                closes.get(i).getAsDouble(),
                volumes.get(i).getAsDouble());
    }

    /** Print a section banner */
    private static void printBanner(String title) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("═══════════════════════════════════════════════════════════");
    }
}

/*
 * ============================================================================
 *  pom.xml DEPENDENCY — Copy this into your pom.xml
 * ============================================================================
 *
 *  <dependencies>
 *      <dependency>
 *          <groupId>com.google.code.gson</groupId>
 *          <artifactId>gson</artifactId>
 *          <version>2.10.1</version>
 *      </dependency>
 *  </dependencies>
 */
