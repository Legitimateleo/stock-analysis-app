package team2.parallax.ui;

import javafx.application.Application;
import javafx.concurrent.Task;
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
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.data.Fortune500;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MainWindow extends Application {

    private MarketDataService marketData;

    // UI components that need to be updated after search
    private ImageView logoView;
    private Label companyNameLabel;
    private Label tickerLabel;
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

    @Override
    public void init() throws Exception {
        // ── Load API key and wire up service ─────────────────────────
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

        // ── Title ─────────────────────────────────────────────────────
        Label title = new Label("PARALLAX");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #1a1a2e;");

        // ── Search bar ────────────────────────────────────────────────
        TextField searchField = new TextField();
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
        tickerLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 14px;");

        countryLabel = new Label();
        countryLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        VBox companyInfo = new VBox(4, companyNameLabel, tickerLabel, countryLabel);
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

        // Separator
        Separator separator = new Separator();

        // Related stocks
        Label relatedTitle = new Label("Related Stocks");
        relatedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        relatedTitle.setStyle("-fx-text-fill: #1a1a2e;");

        relatedStocksPane = new FlowPane();
        relatedStocksPane.setHgap(8);
        relatedStocksPane.setVgap(8);

        resultsPanel.getChildren().addAll(
                companyRow,
                new Separator(),
                metricsGrid,
                separator,
                relatedTitle,
                relatedStocksPane
        );

        // ── Wire search action ────────────────────────────────────────
        searchButton.setOnAction(e -> handleSearch(searchField.getText()));
        searchField.setOnAction(e -> handleSearch(searchField.getText()));

        // ── Assemble root ─────────────────────────────────────────────
        root.getChildren().addAll(title, searchBar, errorLabel, resultsPanel);

        Scene scene = new Scene(root, 700, 600);
        stage.setTitle("Parallax");
        stage.setScene(scene);
        stage.show();
    }

    private void handleSearch(String input) {
        if (input == null || input.trim().isEmpty()) return;

        errorLabel.setVisible(false);
        resultsPanel.setVisible(false);

        // Run search on background thread to avoid freezing UI
        Task<StockSnapshot> task = new Task<>() {
            @Override
            protected StockSnapshot call() {
                return marketData.lookup(input);
            }
        };

        task.setOnSucceeded(e -> {
            StockSnapshot snapshot = task.getValue();
            if (snapshot == null) {
                errorLabel.setText("Stock not found or not in Fortune 500.");
                errorLabel.setVisible(true);
            } else {
                populateResults(snapshot);
                resultsPanel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            errorLabel.setText("Something went wrong. Please try again.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private void populateResults(StockSnapshot snapshot) {
        // Logo
        try {
            Image logo = new Image(snapshot.getLogo(), true);
            logoView.setImage(logo);
        } catch (Exception e) {
            logoView.setImage(null);
        }

        // Company info
        companyNameLabel.setText(snapshot.getCompanyName());
        tickerLabel.setText(snapshot.getTicker());
        countryLabel.setText(snapshot.getCountry());

        // Metrics
        currentPriceLabel.setText("$" + String.format("%.2f", snapshot.getCurrentPrice()));
        peRatioLabel.setText(String.format("%.2f", snapshot.getPeRatio()));
        priceToBookLabel.setText(String.format("%.2f", snapshot.getPriceToBook()));
        dividendYieldLabel.setText(String.format("%.4f", snapshot.getDividendYield()));
        weekHighLabel.setText("$" + String.format("%.2f", snapshot.getWeekHigh52()));
        weekLowLabel.setText("$" + String.format("%.2f", snapshot.getWeekLow52()));

        // Related stocks
        relatedStocksPane.getChildren().clear();
        for (Fortune500 related : snapshot.getRelatedStocks()) {
            Label chip = new Label(related.name());
            chip.setStyle("""
                    -fx-background-color: #e8e8e8;
                    -fx-padding: 4px 10px;
                    -fx-border-radius: 4px;
                    -fx-background-radius: 4px;
                    -fx-font-size: 12px;
                    -fx-cursor: hand;
                    """);
            relatedStocksPane.getChildren().add(chip);
        }
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