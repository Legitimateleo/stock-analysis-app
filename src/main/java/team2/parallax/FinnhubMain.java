package team2.parallax;

import javafx.application.Application;
import team2.parallax.api.FinnhubClient;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.ui.MainWindow;
import team2.parallax.service.CalculationMethods;
import team2.parallax.service.ValidationScore;
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

        StockSnapshot snapshot = marketData.lookup("CI");
        if (snapshot != null) {
            System.out.println("\n── Sector P/E Test ──");
            System.out.println("CIGNA P/E:          " + snapshot.getPeRatio());
            System.out.println("Sector Average P/E: " + snapshot.getSectorAveragePE());
            System.out.println("Free Cash Flow/Share: " + snapshot.getFreeCashFlowPerShare());
        }
        CalculationMethods calculator = new CalculationMethods();
        ValidationScore validationScore = new ValidationScore(calculator);
        validationScore.printSummary(snapshot);

        // ── Launch GUI ───────────────────────────────────────────────
        Application.launch(MainWindow.class, args);
    }
}