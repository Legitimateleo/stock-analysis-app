package team2.parallax;

import team2.parallax.api.FinnhubClient;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.data.Fortune500;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FinnhubMain {

    public static void main(String[] args) {

        //API key Loading up
        Properties config = new Properties();
        try (InputStream input = FinnhubMain.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("ERROR loading config: " + e.getMessage());
            return;
        }

        String apiKey = config.getProperty("FINNHUB_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("ERROR: FINNHUB_API_KEY not found.");
            return;
        }

        // connecting the finnhub client class with the methods within marketData services
        FinnhubClient client = new FinnhubClient(apiKey);
        MarketDataService marketData = new MarketDataService(client);

        // hardcoded user input. this will be used for GUI
        String userInput = "adbe";

        System.out.println("Searching for: " + userInput);
        //connects the snapshot method (the bundler) with the marketDataServices file utilizing the userInput to search
        StockSnapshot snapshot = marketData.lookup(userInput);

        // display result
        if (snapshot == null) {
            System.out.println("Stock not found or not in Fortune 500.");
        } else {
            System.out.println("\n── Result ──────────────────────────");
            System.out.println("Ticker:        " + snapshot.getTicker());
            System.out.println("Company:       " + snapshot.getCompanyName());
            System.out.println("Country:       " + snapshot.getCountry());
            System.out.println("Current Price: $" + snapshot.getCurrentPrice());
            System.out.println("P/E Ratio:     " + snapshot.getPeRatio());
            System.out.println("Price/Book:    " + snapshot.getPriceToBook());
            System.out.println("Div. Yield:    " + snapshot.getDividendYield());
            System.out.println("52W High:      $" + snapshot.getWeekHigh52());
            System.out.println("52W Low:       $" + snapshot.getWeekLow52());
            System.out.println("Logo URL:      " + snapshot.getLogo());
            System.out.println("\n── Related Stocks (" + snapshot.getStock().getIndustry() + ") ──");
            for (Fortune500 related : snapshot.getRelatedStocks()) {
                System.out.println("  " + related.name() + " — " + related.getCompanyName());
            }
        }
    }
}