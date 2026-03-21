package team2.parallax.ui;
import javafx.scene.control.ScrollPane;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import team2.parallax.api.FinnhubClient;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.data.Fortune500;
import team2.parallax.service.ValidationScore;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MainWindow extends Application {

    private MarketDataService marketData;
    private Fortune500 currentStock = null; // add this field at top of class
    // UI components that need to be updated after search
    private ImageView logoView;
    private Label companyNameLabel;
    private Label tickerLabel;
    private Label industryLabel;
    private Label countryLabel;
    private Label currentPriceLabel;
    private Label peRatioLabel;
    private Label priceToBookLabel;
    private Label dividendYieldLabel;
    private Label weekHighLabel;
    private Label weekLowLabel;
    private VBox resultsPanel;
    private FlowPane relatedStocksPane;
    private Label errorLabel;
    private TextField searchField;
    private BarChart<String, Number> recommendationChart;
    private Button trendsButton;
    private Button calculateButton;
    private Label finalScoreLabel;
    private Label signalLabel;
    private StockSnapshot currentSnapshot = null;

    @Override
    public void init() throws Exception {
        Properties config = new Properties();
        try (InputStream input = getClass()
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        }
        String apiKey = config.getProperty("FINNHUB_API_KEY");
        FinnhubClient client = new FinnhubClient(apiKey);
        marketData = new MarketDataService(client);
    }

    @Override
    public void start(Stage stage) {
        // ── Root layout ───────────────────────────────────────────────
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f9f9f9;");

        // ── Wrap root in ScrollPane ───────────────────────────────────
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f9f9f9;");

        Scene sceneScroll = new Scene(scrollPane, 800, 950);
        // ── Title ─────────────────────────────────────────────────────
        Label title = new Label("PARALLAX");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #1a1a2e;");

        // ── Search bar ────────────────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Search by ticker or company name...");
        searchField.setPrefWidth(350);
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");

        Button searchButton = new Button("Search");
        searchButton.setStyle("""
                -fx-background-color: #1a1a2e;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-padding: 8px 20px;
                -fx-cursor: hand;
                """);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
        errorLabel.setVisible(false);


        HBox searchBar = new HBox(10, searchField, searchButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // ── Results panel (hidden until search) ───────────────────────
        resultsPanel = new VBox(15);
        resultsPanel.setVisible(false);
        resultsPanel.setStyle("""
                -fx-background-color: white;
                -fx-padding: 20px;
                -fx-border-color: #e0e0e0;
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
                """);

        // Logo + company info row
        logoView = new ImageView();
        logoView.setFitWidth(60);
        logoView.setFitHeight(60);
        logoView.setPreserveRatio(true);

        companyNameLabel = new Label();
        companyNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        companyNameLabel.setStyle("-fx-text-fill: #1a1a2e;");

        tickerLabel = new Label();
        tickerLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        industryLabel = new Label();
        industryLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        Label tickerIndustrySeparator = new Label("·");
        tickerIndustrySeparator.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");

        HBox tickerIndustryRow = new HBox(6, tickerLabel, tickerIndustrySeparator, industryLabel);
        tickerIndustryRow.setAlignment(Pos.CENTER_LEFT);

        countryLabel = new Label();
        countryLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        VBox companyInfo = new VBox(4, companyNameLabel, tickerIndustryRow, countryLabel);
        companyInfo.setAlignment(Pos.CENTER_LEFT);

        HBox companyRow = new HBox(15, logoView, companyInfo);
        companyRow.setAlignment(Pos.CENTER_LEFT);

        // Metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(40);
        metricsGrid.setVgap(12);
        metricsGrid.setPadding(new Insets(10, 0, 10, 0));

        currentPriceLabel  = new Label();
        peRatioLabel       = new Label();
        priceToBookLabel   = new Label();
        dividendYieldLabel = new Label();
        weekHighLabel      = new Label();
        weekLowLabel       = new Label();

        metricsGrid.add(metricBox("Current Price", currentPriceLabel),  0, 0);
        metricsGrid.add(metricBox("P/E Ratio",     peRatioLabel),       1, 0);
        metricsGrid.add(metricBox("Price/Book",    priceToBookLabel),   0, 1);
        metricsGrid.add(metricBox("Div. Yield",    dividendYieldLabel), 1, 1);
        metricsGrid.add(metricBox("52W High",      weekHighLabel),      0, 2);
        metricsGrid.add(metricBox("52W Low",       weekLowLabel),       1, 2);

        calculateButton = new Button("Calculate Valuation");
        calculateButton.setStyle("""
        -fx-background-color: #1a1a2e;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-padding: 8px 20px;
        -fx-cursor: hand;
        """);
        calculateButton.setVisible(false);
        calculateButton.setOnAction(e -> handleCalculate());

        finalScoreLabel = new Label();
        finalScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        finalScoreLabel.setStyle("-fx-text-fill: #1a1a2e;");

        signalLabel = new Label();
        signalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        // Separator
        Separator separator = new Separator();

        //trendsButton
        trendsButton = new Button("Show Trends");
        trendsButton.setStyle("""
        -fx-background-color: #1a1a2e;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-padding: 8px 20px;
        -fx-cursor: hand;
        """);
        trendsButton.setVisible(false); // hidden until search succeeds
        trendsButton.setOnAction(e -> handleTrends());

        // Related stocks
        Label relatedTitle = new Label("Related Stocks");
        relatedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        relatedTitle.setStyle("-fx-text-fill: #1a1a2e;");

        relatedStocksPane = new FlowPane();
        relatedStocksPane.setHgap(8);
        relatedStocksPane.setVgap(8);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Period");
        yAxis.setLabel("# Analysts");

        recommendationChart = new BarChart<>(xAxis, yAxis);
        recommendationChart.setTitle("Analyst Recommendations");
        recommendationChart.setPrefHeight(300);
        recommendationChart.setAnimated(false);
        recommendationChart.setVisible(false);
        recommendationChart.setStyle("""
        -fx-bar-fill: derive(-fx-accent, 0%);
        """);

        resultsPanel.getChildren().addAll(
                companyRow,
                new Separator(),
                metricsGrid,
                new Separator(),
                calculateButton,
                finalScoreLabel,
                signalLabel,
                new Separator(),
                trendsButton,
                recommendationChart,
                new Separator(),
                relatedTitle,
                relatedStocksPane
        );

        // ── Wire search action ────────────────────────────────────────
        searchButton.setOnAction(e -> handleSearch(searchField.getText()));
        searchField.setOnAction(e -> handleSearch(searchField.getText()));

        // ── Assemble root ─────────────────────────────────────────────
        root.getChildren().addAll(title, searchBar, errorLabel, resultsPanel);

        stage.setTitle("Parallax");
        stage.setMinWidth(800);
        stage.setMinHeight(950);
        stage.setScene(sceneScroll);
        stage.show();
    }


    private void handleSearch(String input) {
        if (input == null || input.trim().isEmpty()) return;

        errorLabel.setVisible(false);
        resultsPanel.setVisible(false);

        // ── Reset chart and scores on new search ──────────────────────
        recommendationChart.getData().clear();
        recommendationChart.setVisible(false);
        finalScoreLabel.setText("");
        signalLabel.setText("");
        currentSnapshot = null;

        Task<StockSnapshot> task = new Task<>() {
            @Override
            protected StockSnapshot call() {
                Fortune500 stock = marketData.search(input);
                if (stock == null) return null;
                currentStock = stock;
                return marketData.getSnapshot(stock);
            }
        };

        task.setOnSucceeded(e -> {
            StockSnapshot snapshot = task.getValue();
            if (snapshot == null) {
                errorLabel.setText("Stock not found or not in Fortune 500.");
                errorLabel.setVisible(true);
            } else {
                currentSnapshot = snapshot;
                populateResults(currentStock, snapshot);
                resultsPanel.setVisible(true);
                trendsButton.setVisible(true);
                calculateButton.setVisible(true);
            }

        });

        task.setOnFailed(e -> {
            errorLabel.setText("Something went wrong. Please try again.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private void handleTrends() {
        if (currentStock == null) return;

        Task<List<RecommendationTrends>> task = new Task<>() {
            @Override
            protected List<RecommendationTrends> call() {
                return marketData.getTrends(currentStock);
            }
        };

        task.setOnSucceeded(e -> {
            List<RecommendationTrends> trends = task.getValue();
            if (trends != null && !trends.isEmpty()) {
                recommendationChart.getData().clear();

                XYChart.Series<String, Number> strongBuySeries  = new XYChart.Series<>();
                XYChart.Series<String, Number> buySeries        = new XYChart.Series<>();
                XYChart.Series<String, Number> holdSeries       = new XYChart.Series<>();
                XYChart.Series<String, Number> sellSeries       = new XYChart.Series<>();
                XYChart.Series<String, Number> strongSellSeries = new XYChart.Series<>();

                strongBuySeries.setName("Strong Buy");
                buySeries.setName("Buy");
                holdSeries.setName("Hold");
                sellSeries.setName("Sell");
                strongSellSeries.setName("Strong Sell");

                for (RecommendationTrends trend : trends) {
                    String period = trend.getPeriod();
                    strongBuySeries.getData().add(new XYChart.Data<>(period, trend.getStrongBuy()));
                    buySeries.getData().add(new XYChart.Data<>(period, trend.getBuy()));
                    holdSeries.getData().add(new XYChart.Data<>(period, trend.getHold()));
                    sellSeries.getData().add(new XYChart.Data<>(period, trend.getSell()));
                    strongSellSeries.getData().add(new XYChart.Data<>(period, trend.getStrongSell()));
                }

                recommendationChart.getData().addAll(
                        strongBuySeries, buySeries, holdSeries, sellSeries, strongSellSeries
                );
                // style each series with distinct colors
                String[] colors = {"#2ecc71", "#27ae60", "#f39c12", "#e74c3c", "#c0392b"};
                for (int i = 0; i < recommendationChart.getData().size(); i++) {
                    XYChart.Series<String, Number> series = recommendationChart.getData().get(i);
                    for (XYChart.Data<String, Number> data : series.getData()) {
                        data.getNode().setStyle(
                                "-fx-bar-fill: " + colors[i] + ";"
                        );
                    }
                }
                recommendationChart.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            errorLabel.setText("Failed to load trends.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private void populateResults(Fortune500 stock, StockSnapshot snapshot) {

        // ── From Fortune500 enum (no API calls) ───────────────────────
        companyNameLabel.setText(stock.getCompanyName());
        tickerLabel.setText(stock.name());
        industryLabel.setText(stock.getIndustry());

        // ── From StockSnapshot (API data) ─────────────────────────────
        try {
            Image logo = new Image(snapshot.getLogo(), true);
            logoView.setImage(logo);
        } catch (Exception e) {
            logoView.setImage(null);
        }

        currentPriceLabel.setText("$" + String.format("%.2f", snapshot.getCurrentPrice()));
        peRatioLabel.setText(String.format("%.2f", snapshot.getPeRatio()));
        priceToBookLabel.setText(String.format("%.2f", snapshot.getPriceToBook()));
        dividendYieldLabel.setText(String.format("%.4f", snapshot.getDividendYield()));
        weekHighLabel.setText("$" + String.format("%.2f", snapshot.getWeekHigh52()));
        weekLowLabel.setText("$" + String.format("%.2f", snapshot.getWeekLow52()));

        // ── Related stocks from enum ───────────────────────────────────
        relatedStocksPane.getChildren().clear();
        for (Fortune500 related : marketData.getByIndustry(stock)) {
            final String relatedName = related.name();
            Label chip = new Label(relatedName);
            chip.setStyle("""
                -fx-background-color: #e8e8e8;
                -fx-padding: 4px 10px;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
                -fx-font-size: 12px;
                -fx-cursor: hand;
                """);
            chip.setOnMouseClicked(event -> {
                searchField.setText(relatedName);
                handleSearch(relatedName);
            });
            relatedStocksPane.getChildren().add(chip);
        }
    }

     // add field at top

    private void handleCalculate() {
        if (currentStock == null || currentSnapshot == null) {
            errorLabel.setText("Please search for a stock first.");
            errorLabel.setVisible(true);
            return;
        }

        ValidationScore valuation = marketData.getValuation(currentStock, currentSnapshot);
        double score  = valuation.getFinalScore(currentStock, currentSnapshot);
        String signal = valuation.getSignal(currentStock, currentSnapshot);

        finalScoreLabel.setText(String.format("Score: %.2f / 10", score));

        String color;
        switch (signal) {
            case "STRONG BUY"  -> color = "#2ecc71";
            case "BUY"         -> color = "#27ae60";
            case "HOLD"        -> color = "#f39c12";
            case "SELL"        -> color = "#e74c3c";
            case "STRONG SELL" -> color = "#c0392b";
            default            -> color = "#1a1a2e";
        }
        signalLabel.setText(signal);
        signalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                + color + ";");
    }


    // Helper to create a labeled metric box
    private VBox metricBox(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        valueLabel.setStyle("-fx-text-fill: #1a1a2e;");
        return new VBox(2, label, valueLabel);
    }
}
