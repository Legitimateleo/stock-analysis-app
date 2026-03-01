package team2.parallax;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import team2.parallax.api.FinnhubClient;
import team2.parallax.service.MarketDataService;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.time.LocalDate;
import java.time.Instant;

public class FinnhubMain {

    public static void main(String[] args) {

        Properties config = new Properties();
        try(InputStream input = FinnhubMain.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                System.out.println("ERROR : config.properties not found in resources");
                return;
            }
            config.load(input);
        }catch (IOException e) {
            System.out.println("ERROR loading config: " + e.getMessage());
            return;
        }

        String apiKey = config.getProperty("FINNHUB_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("ERROR : FINNHUB_API_KEY not found in config.properties");
            return;
        }

        FinnhubClient client = new FinnhubClient(apiKey);
        MarketDataService marketData = new MarketDataService(client);

        String symbol = "NVDA";

        System.out.println("\n---- Quote -------");
        JsonObject quote = marketData.getQuote(symbol);
        if (quote != null) {
            System.out.println("Price  $" + quote.get("c").getAsDouble());
            System.out.println("Change  $" + quote.get("d").getAsDouble());
        }

        System.out.println("\n---- Company Profile -------");
        JsonObject profile = marketData.getCompanyProfile(symbol);
        if (profile != null) {
            System.out.println("Company Name: " + profile.get("name").getAsString());
            System.out.println("Industry: " + profile.get("finnhubIndustry").getAsString());
        }

        System.out.println("\n---- Financial Metrics -------");
        JsonObject metrics = marketData.getFinancialMetrics(symbol);
        if (profile != null) {
            JsonObject m = metrics.getAsJsonObject("metric");
            System.out.println("P/E Ratio: " + m.get("peBasicExclExtraTTM"));
            System.out.println("Beta: " + m.get("beta"));
        }

        System.out.println("\n── Candles (last 30 days) ──");
        long to = Instant.now().getEpochSecond();
        long from = to - (30L * 24 * 60 * 60);
        JsonObject candles = marketData.getCandles(symbol, from, to);
        if (candles != null && "ok".equals(candles.get("s").getAsString())) {
            System.out.println("Data points: " + candles.getAsJsonArray("c").size());
        }

        System.out.println("\n── News ──");
        LocalDate today = LocalDate.now();
        JsonArray news = marketData.getCompanyNews(symbol, today.minusDays(7).toString(), today.toString());
        if (news != null) {
            System.out.println("Articles this week: " + news.size());
        }

        System.out.println("\n── Insider Sentiment ──");
        JsonObject sentiment = marketData.getInsiderSentiment(
                symbol, today.minusMonths(6).toString(), today.toString()
        );
        if (sentiment != null) {
            System.out.println("Sentiment data points: " + sentiment.getAsJsonArray("data").size());
        }

        System.out.println("\n── Insider Transactions ──");
        JsonObject transactions = marketData.getInsiderTransactions(symbol);
        if (transactions != null) {
            System.out.println("Transactions on file: " + transactions.getAsJsonArray("data").size());
        }

        System.out.println("\n✓ Architecture test complete.");
    }
}