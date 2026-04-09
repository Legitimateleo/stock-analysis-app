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

//Version 4/8 -Patrick, Main landing page UI change

public class FinnhubMain {
    public static void main(String[] args) throws IOException {

        // ── Launch GUI ───────────────────────────────────────────────
        Application.launch(MainWindow.class, args);
    }
}
