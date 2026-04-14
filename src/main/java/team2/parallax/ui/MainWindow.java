package team2.parallax.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import team2.parallax.api.FinnhubClient;
import team2.parallax.api.ChartDataClient;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.data.Fortune500;
import team2.parallax.api.PolygonClient;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MainWindow extends Application implements ViewCallBack {

    private ParallaxController controller;

    // UI components
    private ImageView logoView;
    private Label companyNameLabel;
    private Label tickerLabel;
    private Label industryLabel;
    private Label currentPriceLabel;
    private Label priceDifferenceLabel;
    private Label peRatioLabel;
    private Label priceToBookLabel;
    private Label dividendYieldLabel;
    private Label weekHighLabel;
    private Label weekLowLabel;
    private Label marketCapLabel;
    private Label epsLabel;
    private Label grossMarginLabel;
    private Label revenueYoyLabel;
    private VBox resultsPanel;
    private TilePane relatedStocksPane;
    private Label errorLabel;
    private TextField searchField;
    private Pane recommendationChart;
    private Button trendsButton;
    private Button calculateButton;
    private Label finalScoreLabel;
    private Label signalLabel;

    private ChartDataClient polygonClient;
    private StockChartPanel stockChartPanel;

    @Override
    public void init() throws Exception {
        Properties config = new Properties();
        try (InputStream input = getClass()
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        }
        String apiKey = config.getProperty("FINNHUB_API_KEY");
        String polygonKey = config.getProperty("POLYGON_API_KEY", "").trim();
        polygonClient = new PolygonClient(polygonKey);
        stockChartPanel = new StockChartPanel(polygonClient);
        FinnhubClient client = new FinnhubClient(apiKey);
        MarketDataService marketData = new MarketDataService(client);
        controller = new ParallaxController(marketData, this);
    }

    @Override
    public void start(Stage stage) {
        // ── Root layout ───────────────────────────────────────────────
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1a1d24;");
        root.setAlignment(Pos.TOP_CENTER);

        // starting pane
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background: #1a1d24; -fx-background-color: #1a1d24;");
        Scene sceneScroll = new Scene(scrollPane, 1000, 900);

        // ── Title ─────────────────────────────────────────────────────
        Label title = new Label("Parallax");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 64));
        title.setStyle("-fx-text-fill: white;");
        // ── Search bar ────────────────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Search by ticker or company name.     Example: NVDA…");
        searchField.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: white;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Old search color #5cb85c or #51A99B
        Button searchButton = new Button("Search");
        searchButton.setStyle("""
                -fx-background-color: #5cb85c;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-padding: 8px 16px;
                -fx-background-radius: 20px;
                -fx-border-radius: 20px;
                -fx-cursor: hand;
                """);
        searchButton.setOnAction(e -> handleSearch());

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        HBox searchBar = new HBox(8, searchField, searchButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPrefWidth(450);
        searchBar.setMaxWidth(450);
        searchBar.setStyle(
                "-fx-padding: 4px 6px 4px 16px; -fx-background-color: #383d4a; -fx-background-radius: 30px; -fx-border-radius: 30px;");

        // ── Results panel ─────────────────────────────────────────────
        resultsPanel = new VBox(15);
        resultsPanel.setVisible(false);
        resultsPanel.managedProperty().bind(resultsPanel.visibleProperty());
        resultsPanel.setStyle("""
                -fx-background-color: #1a1d24;
                -fx-padding: 20px;
                -fx-border-color: #3f4553;
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
                """);

        // ── Logo + company info ───────────────────────────────────────
        logoView = new ImageView();
        logoView.setFitWidth(60);
        logoView.setFitHeight(60);
        logoView.setPreserveRatio(true);

        companyNameLabel = new Label();
        companyNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        companyNameLabel.setStyle("-fx-text-fill: white;");

        currentPriceLabel = new Label();
        currentPriceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        currentPriceLabel.setStyle("-fx-text-fill: white;");

        priceDifferenceLabel = new Label();
        priceDifferenceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        tickerLabel = new Label();
        tickerLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");

        industryLabel = new Label();
        industryLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");

        Label tickerIndustrySeparator = new Label("·");
        tickerIndustrySeparator.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");

        HBox tickerIndustryRow = new HBox(6, tickerLabel, tickerIndustrySeparator, industryLabel);
        tickerIndustryRow.setAlignment(Pos.CENTER_LEFT);

        HBox priceBox = new HBox(8, currentPriceLabel, priceDifferenceLabel);
        priceBox.setAlignment(Pos.BASELINE_LEFT);

        HBox nameAndPriceRow = new HBox(15, companyNameLabel, priceBox);
        nameAndPriceRow.setAlignment(Pos.BASELINE_LEFT);

        VBox companyInfo = new VBox(4, nameAndPriceRow, tickerIndustryRow);
        companyInfo.setAlignment(Pos.CENTER_LEFT);

        HBox companyRow = new HBox(15, logoView, companyInfo);
        companyRow.setAlignment(Pos.CENTER_LEFT);

        // ── Metrics grid ──────────────────────────────────────────────
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(40);
        metricsGrid.setVgap(12);
        metricsGrid.setPadding(new Insets(10, 0, 10, 0));

        peRatioLabel = new Label();
        priceToBookLabel = new Label();
        dividendYieldLabel = new Label();
        weekHighLabel = new Label();
        weekLowLabel = new Label();
        marketCapLabel = new Label();
        epsLabel = new Label();
        grossMarginLabel = new Label();
        revenueYoyLabel = new Label();

        // Row 1 (Index 0)
        metricsGrid.add(metricBox("P/E Ratio", peRatioLabel), 0, 0);
        metricsGrid.add(metricBox("Market Cap", marketCapLabel), 1, 0);
        metricsGrid.add(metricBox("EPS", epsLabel), 2, 0);
        metricsGrid.add(metricBox("Gross Margin %", grossMarginLabel), 3, 0);
        metricsGrid.add(metricBox("Revenue YoY %", revenueYoyLabel), 4, 0);

        // Row 2 (Index 1)
        metricsGrid.add(metricBox("Div. Yield", dividendYieldLabel), 0, 1);
        metricsGrid.add(metricBox("52W High", weekHighLabel), 1, 1);
        metricsGrid.add(metricBox("52W Low", weekLowLabel), 2, 1);
        metricsGrid.add(metricBox("Price/Book", priceToBookLabel), 3, 1);

        // ── Calculate button + score labels ───────────────────────────
        calculateButton = new Button("Calculate Valuation");
        calculateButton.setStyle("""
                -fx-background-color: #2a2e39;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-padding: 8px 20px;
                -fx-cursor: hand;
                """);
        calculateButton.setVisible(false);
        calculateButton.setOnAction(e -> controller.handleCalculate());

        Tooltip calcTooltip = new Tooltip(
                "Valuation is scored 0–10 by averaging three factors:\n\n"
                        + "1) P/E Score — Lower P/E ratios score higher (e.g. P/E < 8 = 10 pts, "
                        + "P/E > 200 = 1 pt). A low P/E suggests the stock is cheap relative to earnings.\n\n"
                        + "2) 52-Week Position — Compares the current price to the midpoint of its "
                        + "52-week high and low. Trading well below the midpoint scores higher "
                        + "(potential undervaluation).\n\n"
                        + "3) Sector P/E — Compares the stock's P/E to its industry average. "
                        + "A P/E significantly below the sector average scores higher.\n\n"
                        + "Signals:  ≥7 Strong Buy · ≥5.1 Buy · 5 Hold · ≥3 Sell · <3 Strong Sell");
        calcTooltip.setStyle("""
                -fx-background-color: #2a2e39;
                -fx-text-fill: #e0e0e0;
                -fx-font-size: 12px;
                -fx-padding: 10px 14px;
                -fx-background-radius: 6px;
                -fx-border-color: #3f4553;
                -fx-border-radius: 6px;
                """);
        calcTooltip.setWrapText(true);
        calcTooltip.setMaxWidth(500);
        calcTooltip.setShowDelay(javafx.util.Duration.millis(200));
        calcTooltip.setShowDuration(javafx.util.Duration.seconds(30));
        calcTooltip.setHideDelay(javafx.util.Duration.millis(100));
        calculateButton.setTooltip(calcTooltip);

        finalScoreLabel = new Label();
        finalScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        finalScoreLabel.setStyle("-fx-text-fill: white;");

        signalLabel = new Label();
        signalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // ── Trends button ─────────────────────────────────────────────
        trendsButton = new Button("Show Trends");
        trendsButton.setStyle("""
                -fx-background-color: #2a2e39;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-padding: 8px 20px;
                -fx-cursor: hand;
                """);
        trendsButton.setVisible(false);
        trendsButton.setOnAction(e -> {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            controller.handleTrends();
        });

        // ── Recommendation chart placeholder ──────────────────────────
        recommendationChart = new Pane();
        recommendationChart.setVisible(false);

        // ── Stock Chart Panel ─────────────────────────────────────────
        stockChartPanel = new StockChartPanel(polygonClient);
        stockChartPanel.setTimeframeChangeListener((timeframe, oldPrice, currentPrice) -> {
            Platform.runLater(() -> {
                double diff = currentPrice - oldPrice;
                double pct = (oldPrice > 0) ? (diff / oldPrice) * 100.0 : 0;
                String sign = diff >= 0 ? "+" : "";
                String color = diff >= 0 ? "#4cd137" : "#e74c3c";

                currentPriceLabel.setText(String.format("%.2f", currentPrice));
                currentPriceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

                priceDifferenceLabel.setText(String.format("%s%.2f ( %s%.2f %% )", sign, diff, sign, pct));
                priceDifferenceLabel
                        .setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            });
        });

        // ── Related stocks ────────────────────────────────────────────
        Label relatedTitle = new Label("Related Stocks");
        relatedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        relatedTitle.setStyle("-fx-text-fill: white;");

        relatedStocksPane = new TilePane();
        relatedStocksPane.setHgap(8);
        relatedStocksPane.setVgap(8);
        relatedStocksPane.setPrefColumns(2);

        // Sidebar related stocks + image testing has to be 180+ for 2 vertical
        VBox relatedBox = new VBox(10, relatedTitle, relatedStocksPane);
        relatedBox.setMinWidth(180);
        relatedBox.setMaxWidth(200);

        HBox chartAndRelatedBox = new HBox(20, stockChartPanel, relatedBox);
        HBox.setHgrow(stockChartPanel, Priority.ALWAYS);

        // ── Assemble results panel ────────────────────────────────────
        resultsPanel.getChildren().addAll(
                companyRow,
                new Separator(),
                chartAndRelatedBox,
                new Separator(),
                metricsGrid,
                new Separator(),
                calculateButton,
                finalScoreLabel,
                signalLabel,
                new Separator(),
                trendsButton,
                recommendationChart);

        // ── Wire search action ────────────────────────────────────────
        searchField.setOnAction(e -> handleSearch());

        // ── Assemble root ─────────────────────────────────────────────
        Region topSpacer = new Region();
        topSpacer.prefHeightProperty().bind(scrollPane.heightProperty().multiply(0.15));

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        bottomSpacer.setMinHeight(200);

        resultsPanel.visibleProperty().addListener((obs, oldV, newV) -> {
            if (newV && topSpacer.isVisible()) {
                topSpacer.setVisible(false);
                topSpacer.setManaged(false);
                title.setFont(Font.font("SansSerif", FontWeight.BOLD, 28));
                root.setAlignment(Pos.TOP_LEFT);
                searchBar.setAlignment(Pos.CENTER_LEFT);
            }
        });

        root.getChildren().addAll(topSpacer, title, searchBar, errorLabel, resultsPanel, bottomSpacer);

        stage.setTitle("Parallax");
        stage.setMinWidth(800);
        stage.setMinHeight(950);
        stage.setScene(sceneScroll);
        stage.show();
    }

    // ── View trigger methods ──────────────────────────────────────────
    private void handleSearch() {
        errorLabel.setVisible(false);
        resultsPanel.setVisible(false);
        finalScoreLabel.setText("");
        signalLabel.setText("");

        // reset chart
        int chartIndex = resultsPanel.getChildren().indexOf(recommendationChart);
        recommendationChart = new Pane();
        recommendationChart.setVisible(false);
        if (chartIndex >= 0) {
            resultsPanel.getChildren().set(chartIndex, recommendationChart);
        }

        controller.handleSearch(searchField.getText());
    }

    // ── ViewCallback implementations ──────────────────────────────────
    @Override
    public void onSearchSuccess(Fortune500 stock, StockSnapshot snapshot) {
        Platform.runLater(() -> {
            populateResults(stock, snapshot);
            controller.handleChartLoad(stock.name()); // ← through controller
            resultsPanel.setVisible(true);
            trendsButton.setVisible(true);
            calculateButton.setVisible(true);
            errorLabel.setVisible(false);
        });
    }

    @Override
    public void onSearchFailure(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    @Override
    public void onTrendsLoaded(List<RecommendationTrends> trends) {
        Platform.runLater(() -> {
            errorLabel.setText("");
            errorLabel.setVisible(false);

            int chartIndex = resultsPanel.getChildren().indexOf(recommendationChart);
            recommendationChart = RecommendationTrendsChart.build(
                    trends, controller.getCurrentStock().name());
            recommendationChart.setVisible(true);

            if (chartIndex >= 0) {
                resultsPanel.getChildren().set(chartIndex, recommendationChart);
            }
        });
    }

    @Override
    public void onTrendsLoadFailure(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    @Override
    public void onScoreCalculated(double score, String signal) {
        Platform.runLater(() -> {
            finalScoreLabel.setText(String.format("Score: %.2f / 10", score));
            String color;
            switch (signal) {
                case "STRONG BUY" -> color = "#4cd137";
                case "BUY" -> color = "#2ecc71";
                case "HOLD" -> color = "#f1c40f";
                case "SELL" -> color = "#e74c3c";
                case "STRONG SELL" -> color = "#c0392b";
                default -> color = "white";
            }
            signalLabel.setText(signal);
            signalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                    + color + ";");
        });
    }

    @Override
    public void onScoreCalculatedFailure(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    @Override
    public void onChartLoad(String ticker) {
        stockChartPanel.load(ticker);
    }

    // ── Private view helpers ──────────────────────────────────────────
    private void populateResults(Fortune500 stock, StockSnapshot snapshot) {

        // ── From Fortune500 enum ──────────────────────────────────────
        companyNameLabel.setText(stock.getCompanyName());
        tickerLabel.setText(stock.name());
        industryLabel.setText(stock.getIndustry());

        // ── From StockSnapshot ────────────────────────────────────────
        try {
            Image logo = new Image(snapshot.getLogo(), true);
            logoView.setImage(logo);
        } catch (Exception e) {
            logoView.setImage(null);
        }

        // Price will be overwritten by Polygon's listener, but initialize Finnhub
        // values
        double chg = snapshot.getChange();
        double pct = snapshot.getChangePercent();
        String sign = chg >= 0 ? "+" : "";
        String color = chg >= 0 ? "#4cd137" : "#e74c3c";
        currentPriceLabel.setText(String.format("%.2f", snapshot.getCurrentPrice()));
        currentPriceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        priceDifferenceLabel.setText(String.format("%s%.2f$ (%s%.2f%% )", sign, chg, sign, pct));
        priceDifferenceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        peRatioLabel.setText(String.format("%.2f", snapshot.getPeRatio()));
        priceToBookLabel.setText(String.format("%.2f", snapshot.getPriceToBook()));
        dividendYieldLabel.setText(String.format("%.4f", snapshot.getDividendYield()));
        weekHighLabel.setText("$" + String.format("%.2f", snapshot.getWeekHigh52()));
        weekLowLabel.setText("$" + String.format("%.2f", snapshot.getWeekLow52()));

        double mc = snapshot.getMarketCap();
        String mcStr = (mc >= 1000) ? String.format("%.2fB", mc / 1000) : String.format("%.0fM", mc);
        marketCapLabel.setText(mcStr);
        epsLabel.setText(String.format("$%.2f", snapshot.getEps()));
        grossMarginLabel.setText(String.format("%.2f%%", snapshot.getGrossMargin()));
        revenueYoyLabel.setText(String.format("%.2f%%", snapshot.getRevenueYoy()));

        // ── Related stocks from enum ──────────────────────────────────
        relatedStocksPane.getChildren().clear();
        List<Fortune500> relatedList = controller.getMarketData().getByIndustry(stock);
        for (Fortune500 related : relatedList) {
            final String relatedName = related.name();
            Label chip = new Label(relatedName);
            chip.setStyle("""
                    -fx-background-color: #2a2e39;
                    -fx-text-fill: white;
                    -fx-padding: 4px 10px;
                    -fx-border-radius: 4px;
                    -fx-background-radius: 4px;
                    -fx-font-size: 12px;
                    -fx-cursor: hand;
                    """);
            chip.setOnMouseClicked(event -> {
                searchField.setText(relatedName);
                handleSearch();
            });
            relatedStocksPane.getChildren().add(chip);
        }

        // Fetch logos sequentially in background to respect rate limits
        new Thread(() -> {
            for (int i = 0; i < relatedList.size(); i++) {
                String relatedName = relatedList.get(i).name();
                String logoUrl = controller.getMarketData().getLogoUrl(relatedName);
                if (logoUrl != null && !logoUrl.equals("N/A") && !logoUrl.isEmpty()) {
                    Image icon = new Image(logoUrl, true);
                    final int index = i;
                    Platform.runLater(() -> {
                        // Ensure we are still showing the same related stocks (user didn't search
                        // again)
                        if (relatedStocksPane.getChildren().size() > index) {
                            javafx.scene.Node node = relatedStocksPane.getChildren().get(index);
                            if (node instanceof Label chip && chip.getText().equals(relatedName)) {
                                ImageView chipIcon = new ImageView(icon);
                                chipIcon.setFitWidth(16);
                                chipIcon.setFitHeight(16);
                                chipIcon.setPreserveRatio(true);
                                chip.setGraphic(chipIcon);
                                chip.setGraphicTextGap(6);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private VBox metricBox(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        valueLabel.setStyle("-fx-text-fill: white;");

        VBox box = new VBox(2, label, valueLabel);

        String description = getMetricDescription(labelText);
        if (description != null) {
            Tooltip tooltip = new Tooltip(description);
            tooltip.setStyle("""
                    -fx-background-color: #2a2e39;
                    -fx-text-fill: #e0e0e0;
                    -fx-font-size: 12px;
                    -fx-padding: 8px 12px;
                    -fx-background-radius: 6px;
                    -fx-border-color: #3f4553;
                    -fx-border-radius: 6px;
                    """);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(280);
            tooltip.setShowDelay(javafx.util.Duration.millis(200));
            tooltip.setShowDuration(javafx.util.Duration.seconds(15));
            tooltip.setHideDelay(javafx.util.Duration.millis(100));
            Tooltip.install(box, tooltip);

            box.setOnMouseEntered(
                    e -> box.setStyle("-fx-background-color: #2a2e39; -fx-background-radius: 6px; -fx-padding: 4px;"));
            box.setOnMouseExited(e -> box.setStyle("-fx-background-color: transparent; -fx-padding: 4px;"));
            box.setStyle("-fx-padding: 4px;");
        }

        return box;
    }

    // hard code text descriptions
    private String getMetricDescription(String metric) {
        return switch (metric) {
            case "P/E Ratio" ->
                "Price-to-Earnings ratio compares a company's stock price to its earnings per share. A higher P/E may indicate expected future growth, while a lower P/E can signal undervaluation.";
            case "Market Cap" ->
                "Market capitalization is the total value of a company's outstanding shares. It reflects the company's size and is used to classify stocks as large-cap, mid-cap, or small-cap.";
            case "EPS" ->
                "Earnings Per Share represents the portion of a company's profit allocated to each share. Higher EPS generally indicates greater profitability.";
            case "Gross Margin %" ->
                "Gross margin measures the percentage of revenue remaining after subtracting the cost of goods sold. A higher margin indicates better efficiency in production.";
            case "Revenue YoY %" ->
                "Revenue Year-over-Year growth shows how much a company's revenue has increased or decreased compared to the same period last year.";
            case "Div. Yield" ->
                "Dividend yield is the annual dividend payment divided by the stock price, expressed as a percentage. It shows how much income you earn per dollar invested.";
            case "52W High" ->
                "The 52-week high is the highest price the stock has traded at during the past year. It helps gauge how close the stock is to its recent peak.";
            case "52W Low" ->
                "The 52-week low is the lowest price the stock has traded at during the past year. It helps assess how far the stock has recovered from its recent bottom.";
            case "Price/Book" ->
                "Price-to-Book ratio compares a stock's market price to its book value (net assets). A lower ratio may indicate the stock is undervalued relative to its assets.";
            default -> null;
        };
    }
}
