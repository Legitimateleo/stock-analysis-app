package team2.parallax;

import team2.parallax.api.FinnhubClient;
import team2.parallax.api.PolygonClient;
import team2.parallax.service.MarketDataService;
import team2.parallax.ui.MainWindow;

import java.io.InputStream;
import java.util.Properties;

public class ParallaxApp {

    public static void main(String[] args) throws Exception {
        // Load config
        Properties config = new Properties();
        try (InputStream input = ParallaxApp.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        }

        // Build API layer
        String apiKey = config.getProperty("FINNHUB_API_KEY");
        String polygonKey = config.getProperty("POLYGON_API_KEY", "").trim();
        FinnhubClient finnhubClient = new FinnhubClient(apiKey);
        PolygonClient polygonClient = new PolygonClient(polygonKey);

        // Build service layer
        MarketDataService marketData = new MarketDataService(finnhubClient);

        // Wire and launch
        MainWindow.launch( polygonClient, marketData);
    }
}