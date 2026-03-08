package team2.parallax;

import javafx.application.Application;
import team2.parallax.api.FinnhubClient;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.ui.MainWindow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FinnhubMain {
    public static void main(String[] args) throws IOException {

        // ── Temporary test ───────────────────────────────────────────
        Properties config = new Properties();
        try (InputStream input = FinnhubMain.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        }
        String apiKey = config.getProperty("FINNHUB_API_KEY");
        FinnhubClient client = new FinnhubClient(apiKey);
        MarketDataService marketData = new MarketDataService(client);

        StockSnapshot snapshot = marketData.lookup("CAT");
        if (snapshot != null) {
            System.out.println("\n── Recommendation Trends ──");
            for (RecommendationTrends trend : snapshot.getRecommendationTrends()) {
                System.out.println(trend.toString());
            }
        }

        // ── Launch GUI ───────────────────────────────────────────────
        Application.launch(MainWindow.class, args);
    }
}