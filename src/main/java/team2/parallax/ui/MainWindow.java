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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import team2.parallax.api.FinnhubClient;
import team2.parallax.api.PolygonClient;
import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataService;
import team2.parallax.service.PolygonCandleService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MainWindow extends Application {

    // ── Colour palette (matches Google Finance dark) ──────────────────────────
    private static final String BG_DEEP = "#111111";
    private static final String BG_SURFACE = "#1c1c1c";
    private static final String BG_CARD = "#242424";
    private static final String BG_HOVER = "#2e2e2e";
    private static final String BORDER_CLR = "#3a3a3a";
    private static final String TEXT_PRI = "#e8eaed";
    private static final String TEXT_SEC = "#9aa0a6";
    private static final String TEXT_MUT = "#5f6368";
    private static final String ACCENT = "#4285f4"; // Google blue
    private static final String GREEN = "#0d904f";
    private static final String GREEN_TXT = "#34a853";
    private static final String RED_TXT = "#ea4335";
    private static final String DIVIDER = "#2f2f2f";

    // ── State ─────────────────────────────────────────────────────────────────
    private MarketDataService   marketData;
    private PolygonCandleService polygonService;

    // ── Live UI refs ──────────────────────────────────────────────────────────
    private TextField searchField;
    private Label errorLabel;

    // Left sidebar
    private VBox sectorListBox;

    // Centre – header
    private Label companyNameLabel;
    private Label exchangeLabel;
    private Label currentPriceLabel;
    private Label priceChangeLabel;
    private Label pricePctLabel;
    private ImageView logoView;

    // Centre – metrics table
    private GridPane metricsGrid;

    // Centre – stock price chart (rebuilt on each search)
    private VBox stockChartContainer;
    private StockChart currentStockChart;  // tracked so we can cancel in-flight callbacks

    // Centre – recommendation chart area
    private VBox chartArea;
    private javafx.scene.layout.Region recommendationChart;

    // Right – related/overview
    private VBox rightPanel;
    private Label rightTitle;
    private FlowPane relatedStocksPane;
    private VBox overviewSection;

    // Visibility toggle
    private VBox centreContent;
    private VBox welcomeBox;    // hidden once first result loads

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

        String polygonKey = config.getProperty("POLYGON_API_KEY", "").trim();
        polygonService = new PolygonCandleService(new PolygonClient(polygonKey));
    }

    @Override
    public void start(Stage stage) {

        // ── Top nav ───────────────────────────────────────────────────────────
        HBox topNav = buildTopNav();

        // ── Three-column body ─────────────────────────────────────────────────
        HBox body = new HBox();
        body.setStyle("-fx-background-color: " + BG_DEEP + ";");

        // Left sidebar
        VBox leftSidebar = buildLeftSidebar();
        leftSidebar.setPrefWidth(220);
        leftSidebar.setMinWidth(200);

        // Centre panel
        VBox centre = buildCentrePanel();
        HBox.setHgrow(centre, Priority.ALWAYS);

        // Right panel
        rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(310);
        rightPanel.setMinWidth(280);

        body.getChildren().addAll(leftSidebar, centre, rightPanel);

        // ── Root ──────────────────────────────────────────────────────────────
        VBox root = new VBox(topNav, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        root.setStyle("-fx-background-color: " + BG_DEEP + ";");

        // ── Error toast ───────────────────────────────────────────────────────
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + RED_TXT + "; -fx-font-size: 13px;"
                + "-fx-padding: 6px 16px; -fx-background-color: #2c1a1a;"
                + "-fx-background-radius: 4px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        StackPane stackRoot = new StackPane(root, errorLabel);
        StackPane.setAlignment(errorLabel, Pos.TOP_CENTER);
        StackPane.setMargin(errorLabel, new Insets(60, 0, 0, 0));

        Scene scene = new Scene(stackRoot, 1280, 820);
        stage.setTitle("Parallax — Stock Research");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    // =========================================================================
    // TOP NAV
    // =========================================================================
    private HBox buildTopNav() {
        // Logo / brand
        Label brand = new Label("Parallax");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        brand.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        Label betaBadge = new Label("Beta");
        betaBadge.setStyle("-fx-background-color: " + GREEN + ";"
                + "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-padding: 1px 6px; -fx-background-radius: 4px;");

        HBox brandBox = new HBox(8, brand, betaBadge);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        // Search
        searchField = new TextField();
        searchField.setPromptText("Search stocks...");
        searchField.setPrefWidth(340);
        searchField.setStyle(
                "-fx-background-color: #2a2a2a;"
                        + "-fx-text-fill: " + TEXT_PRI + ";"
                        + "-fx-prompt-text-fill: " + TEXT_MUT + ";"
                        + "-fx-padding: 7px 14px;"
                        + "-fx-font-size: 14px;"
                        + "-fx-border-color: " + BORDER_CLR + ";"
                        + "-fx-border-radius: 24px;"
                        + "-fx-background-radius: 24px;");

        Button searchBtn = new Button("Search");
        searchBtn.setStyle(
                "-fx-background-color: " + ACCENT + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 7px 20px;"
                        + "-fx-background-radius: 24px;"
                        + "-fx-cursor: hand;");
        searchBtn.setOnAction(e -> handleSearch(searchField.getText()));
        searchField.setOnAction(e -> handleSearch(searchField.getText()));

        searchBtn.setOnMouseEntered(e -> searchBtn.setStyle(
                "-fx-background-color: #3b78e7;"
                        + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                        + "-fx-padding: 7px 20px; -fx-background-radius: 24px; -fx-cursor: hand;"));
        searchBtn.setOnMouseExited(e -> searchBtn.setStyle(
                "-fx-background-color: " + ACCENT + ";"
                        + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                        + "-fx-padding: 7px 20px; -fx-background-radius: 24px; -fx-cursor: hand;"));

        HBox searchBox = new HBox(8, searchField, searchBtn);
        searchBox.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(24, brandBox, spacer, searchBox);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(0, 20, 0, 20));
        nav.setPrefHeight(56);
        nav.setStyle("-fx-background-color: " + BG_SURFACE + ";"
                + "-fx-border-color: " + DIVIDER + ";"
                + "-fx-border-width: 0 0 1 0;");
        return nav;
    }

    // =========================================================================
    // LEFT SIDEBAR
    // =========================================================================
    private VBox buildLeftSidebar() {
        Label listsHeader = sectionHeader("Lists");
        Label watchlistTitle = new Label("Watchlist");
        watchlistTitle.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 13px;"
                + "-fx-padding: 4px 0 0 0;");
        Label watchlistEmpty = new Label("Search for a stock to add it to your watchlist.");
        watchlistEmpty.setWrapText(true);
        watchlistEmpty.setStyle("-fx-text-fill: " + TEXT_MUT + "; -fx-font-size: 12px;"
                + "-fx-padding: 4px 0 0 0;");

        Label sectorHeader = sectionHeader("Equity sectors");
        sectorListBox = new VBox(0);

        // Real Fortune500 industries → representative leading ticker
        // Each entry: {Display Name, Fortune500 ticker to search on click, sub-label}
        Object[][] sectors = {
                { "Health Care", Fortune500.UNH, "UNH · UnitedHealth" },
                { "Technology", Fortune500.AAPL, "AAPL · Apple" },
                { "Energy", Fortune500.XOM, "XOM · Exxon Mobil" },
                { "Banking", Fortune500.JPM, "JPM · JPMorgan" },
                { "Retail", Fortune500.WMT, "WMT · Walmart" },
                { "Financial Services", Fortune500.GS, "GS · Goldman Sachs" },
                { "Pharmaceuticals", Fortune500.JNJ, "JNJ · J&J" },
                { "Semiconductors", Fortune500.NVDA, "NVDA · NVIDIA" },
                { "Media", Fortune500.GOOGL, "GOOGL · Alphabet" },
                { "Aerospace & Defense", Fortune500.RTX, "RTX · RTX Corp" },
                { "Insurance", Fortune500.PGR, "PGR · Progressive" },
                { "Automobiles", Fortune500.TSLA, "TSLA · Tesla" },
                { "Utilities", Fortune500.NEE, "NEE · NextEra" },
                { "Biotechnology", Fortune500.ABBV, "ABBV · AbbVie" },
                { "Logistics & Transport", Fortune500.UPS, "UPS · UPS" },
        };
        for (Object[] s : sectors) {
            Fortune500 rep = (Fortune500) s[1];
            String display = (String) s[0];
            String subLabel = (String) s[2];
            sectorListBox.getChildren().add(
                    sectorRow(display.toUpperCase(), subLabel, rep));
        }

        VBox sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: " + BG_SURFACE + ";"
                + "-fx-border-color: " + DIVIDER + ";"
                + "-fx-border-width: 0 1 0 0;");
        sidebar.setPadding(new Insets(16, 0, 16, 0));

        VBox watchlistBox = new VBox(4, watchlistTitle, watchlistEmpty);
        watchlistBox.setPadding(new Insets(0, 16, 12, 16));

        HBox listHdr = new HBox(listsHeader);
        listHdr.setPadding(new Insets(0, 16, 6, 16));

        HBox secHdr = new HBox(sectorHeader);
        secHdr.setPadding(new Insets(8, 16, 4, 16));

        ScrollPane sectorScroll = new ScrollPane(sectorListBox);
        sectorScroll.setFitToWidth(true);
        sectorScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + "-fx-padding: 0;");
        sectorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(sectorScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(listHdr, watchlistBox, secHdr, sectorScroll);
        return sidebar;
    }

    /**
     * Builds a clickable sector row that triggers a stock search for {@code rep}.
     * 
     * @param industryName ALL-CAPS industry label
     * @param subLabel     smaller descriptor shown below (e.g. "AAPL · Apple")
     * @param rep          the Fortune500 entry to search when clicked
     */
    private HBox sectorRow(String industryName, String subLabel, Fortune500 rep) {
        Label nameLbl = new Label(industryName);
        nameLbl.setStyle("-fx-text-fill: " + TEXT_PRI + "; -fx-font-size: 11px;"
                + "-fx-font-weight: bold;");
        Label tickerLbl = new Label(subLabel);
        tickerLbl.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 10px;");
        VBox nameBlock = new VBox(1, nameLbl, tickerLbl);
        nameBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        // Arrow indicator to signal interactivity
        Label arrowLbl = new Label("›");
        arrowLbl.setStyle("-fx-text-fill: " + TEXT_MUT + "; -fx-font-size: 14px;");

        HBox row = new HBox(0, nameBlock, arrowLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 16));
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + BG_HOVER + "; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            searchField.setText(rep.name());
            handleSearch(rep.name());
        });
        return row;
    }

    private Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lbl.setStyle("-fx-text-fill: " + TEXT_PRI + ";");
        return lbl;
    }

    // =========================================================================
    // CENTRE PANEL
    // =========================================================================
    private VBox buildCentrePanel() {
        // Minimal placeholder shown before any search (just the chart icon)
        Label welcomeIcon = new Label("📈");
        welcomeIcon.setStyle("-fx-font-size: 40px; -fx-opacity: 0.3;");
        welcomeBox = new VBox(welcomeIcon);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setPadding(new Insets(120, 24, 24, 24));
        VBox.setVgrow(welcomeBox, Priority.ALWAYS);

        // ── Company header ────────────────────────────────────────────────────
        logoView = new ImageView();
        logoView.setFitWidth(36);
        logoView.setFitHeight(36);
        logoView.setPreserveRatio(true);

        // Circular clip for logo
        Circle clip = new Circle(18, 18, 18);
        logoView.setClip(clip);

        companyNameLabel = new Label();
        companyNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        companyNameLabel.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        exchangeLabel = new Label();
        exchangeLabel.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 13px;");

        VBox companyMeta = new VBox(2, companyNameLabel, exchangeLabel);
        companyMeta.setAlignment(Pos.CENTER_LEFT);

        HBox companyHeaderRow = new HBox(12, logoView, companyMeta);
        companyHeaderRow.setAlignment(Pos.CENTER_LEFT);

        // ── Price row ─────────────────────────────────────────────────────────
        currentPriceLabel = new Label();
        currentPriceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        currentPriceLabel.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        priceChangeLabel = new Label();
        priceChangeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));

        pricePctLabel = new Label();
        pricePctLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));

        HBox priceRow = new HBox(12, currentPriceLabel, priceChangeLabel, pricePctLabel);
        priceRow.setAlignment(Pos.BOTTOM_LEFT);

        // ── Metrics table (like Google Finance overview) ─────────────────────────────
        metricsGrid = new GridPane();
        metricsGrid.setHgap(0);
        metricsGrid.setVgap(0);
        metricsGrid.setPadding(new Insets(16, 0, 0, 0));
        // Outer border only – cell borders drawn by metricsCell()
        metricsGrid.setStyle("-fx-border-color: " + DIVIDER + "; -fx-border-width: 1 1 0 1;");
        // 2 equal columns; the divider is handled by right-border CSS on the left cell
        ColumnConstraints ccL = new ColumnConstraints();
        ccL.setPercentWidth(50);
        ccL.setHgrow(Priority.ALWAYS);
        ColumnConstraints ccR = new ColumnConstraints();
        ccR.setPercentWidth(50);
        ccR.setHgrow(Priority.ALWAYS);
        metricsGrid.getColumnConstraints().addAll(ccL, ccR);

        // ── Stock price chart placeholder (populated on search) ───────────────
        stockChartContainer = new VBox();
        stockChartContainer.setStyle("-fx-background-color: " + BG_DEEP + ";");
        stockChartContainer.setPadding(new Insets(0, 0, 8, 0));

        // ── Recommendation chart placeholder ──────────────────────────────────
        recommendationChart = new javafx.scene.layout.Pane();
        recommendationChart.setVisible(false);
        recommendationChart.setManaged(false);

        chartArea = new VBox(0, recommendationChart);
        chartArea.setPadding(new Insets(16, 0, 0, 0));

        // ── Assembled content (hidden until search) ────────────────────────────
        // Order: company header → price → stock chart → metrics → rec chart
        centreContent = new VBox(16,
                companyHeaderRow,
                priceRow,
                stockChartContainer,
                new Separator() {
                    {
                        setStyle("-fx-background-color: " + DIVIDER + "; -fx-opaque-insets: 0;");
                    }
                },
                metricsGrid,
                chartArea);
        centreContent.setPadding(new Insets(24, 24, 24, 24));
        centreContent.setVisible(false);
        centreContent.setManaged(false);

        VBox centre = new VBox(0, welcomeBox, centreContent);
        centre.setStyle("-fx-background-color: " + BG_DEEP + ";");
        ScrollPane centreScroll = new ScrollPane(centre);
        centreScroll.setFitToWidth(true);
        centreScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        centreScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox wrapper = new VBox(centreScroll);
        VBox.setVgrow(centreScroll, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color: " + BG_DEEP + ";");
        return wrapper;
    }

    // =========================================================================
    // RIGHT PANEL
    // =========================================================================
    private VBox buildRightPanel() {
        rightTitle = new Label("Related Stocks");
        rightTitle.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        rightTitle.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        relatedStocksPane = new FlowPane();
        relatedStocksPane.setHgap(6);
        relatedStocksPane.setVgap(6);

        overviewSection = new VBox(8);

        VBox content = new VBox(16, rightTitle, relatedStocksPane, overviewSection);
        content.setPadding(new Insets(24, 16, 24, 16));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox panel = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: " + BG_SURFACE + ";"
                + "-fx-border-color: " + DIVIDER + ";"
                + "-fx-border-width: 0 0 0 1;");
        return panel;
    }

    // =========================================================================
    // SEARCH HANDLER
    // =========================================================================
    private void handleSearch(String input) {
        if (input == null || input.trim().isEmpty())
            return;

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Task<StockSnapshot> task = new Task<>() {
            @Override
            protected StockSnapshot call() {
                return marketData.lookup(input);
            }
        };

        task.setOnSucceeded(e -> {
            StockSnapshot snapshot = task.getValue();
            if (snapshot == null) {
                showError("Stock not found or not in Fortune 500.");
            } else {
                populateResults(snapshot);
                // Hide the welcome placeholder, reveal the result panel
                welcomeBox.setVisible(false);
                welcomeBox.setManaged(false);
                centreContent.setVisible(true);
                centreContent.setManaged(true);
            }
        });

        task.setOnFailed(e -> showError("Something went wrong. Please try again."));

        new Thread(task).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // =========================================================================
    // POPULATE RESULTS
    // =========================================================================
    private void populateResults(StockSnapshot snap) {
        // Logo
        try {
            Image logo = new Image(snap.getLogo(), true);
            logoView.setImage(logo);
        } catch (Exception ex) {
            logoView.setImage(null);
        }

        // Header
        companyNameLabel.setText(snap.getCompanyName());
        exchangeLabel.setText(snap.getTicker() + " · " + snap.getStock().getIndustry()
                + " · " + snap.getCountry());

        // Price
        double price = snap.getCurrentPrice();
        currentPriceLabel.setText(String.format("$%.2f", price));
        priceChangeLabel.setText("");
        pricePctLabel.setText("");

        // ── Stock price chart (rebuilt for the new ticker) ───────────────
        // Cancel the old chart BEFORE clearing the container, so any pending
        // Platform.runLater callbacks from the previous instance are no-ops
        // and cannot cause NPE in JavaFX's synchronizeSceneNodes.
        if (currentStockChart != null)
            currentStockChart.cancel();
        stockChartContainer.getChildren().clear();
        currentStockChart = new StockChart(polygonService, snap.getTicker());
        stockChartContainer.getChildren().add(currentStockChart.build());

        // Metrics grid
        metricsGrid.getChildren().clear();

        Object[][] rows = {
                // {label, value, label, value}
                { "P/E Ratio", fmt2(snap.getPeRatio()),
                        "Price/Book", fmt2(snap.getPriceToBook()) },
                { "Div. Yield", fmt4(snap.getDividendYield()),
                        "52W High", "$" + fmt2(snap.getWeekHigh52()) },
                { "52W Low", "$" + fmt2(snap.getWeekLow52()),
                        "Current Price", "$" + fmt2(snap.getCurrentPrice()) },
        };

        for (int r = 0; r < rows.length; r++) {
            // Left cell: bottom + right border (right border = vertical divider)
            VBox leftCell = metricsCell(
                    (String) rows[r][0], (String) rows[r][1], true);
            metricsGrid.add(leftCell, 0, r);
            // Right cell: bottom border only
            VBox rightCell = metricsCell(
                    (String) rows[r][2], (String) rows[r][3], false);
            metricsGrid.add(rightCell, 1, r);
        }

        // Related stocks (right panel)
        relatedStocksPane.getChildren().clear();
        for (Fortune500 related : snap.getRelatedStocks()) {
            Label chip = new Label(related.name());
            chip.setStyle(
                    "-fx-background-color: " + BG_CARD + ";"
                            + "-fx-text-fill: " + ACCENT + ";"
                            + "-fx-padding: 4px 12px;"
                            + "-fx-border-radius: 16px;"
                            + "-fx-background-radius: 16px;"
                            + "-fx-font-size: 12px;"
                            + "-fx-cursor: hand;"
                            + "-fx-border-color: " + BORDER_CLR + ";"
                            + "-fx-border-width: 1;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                    "-fx-background-color: " + BG_HOVER + ";"
                            + "-fx-text-fill: " + ACCENT + ";"
                            + "-fx-padding: 4px 12px; -fx-border-radius: 16px;"
                            + "-fx-background-radius: 16px; -fx-font-size: 12px;"
                            + "-fx-cursor: hand; -fx-border-color: " + ACCENT
                            + "; -fx-border-width: 1;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                    "-fx-background-color: " + BG_CARD + ";"
                            + "-fx-text-fill: " + ACCENT + ";"
                            + "-fx-padding: 4px 12px; -fx-border-radius: 16px;"
                            + "-fx-background-radius: 16px; -fx-font-size: 12px;"
                            + "-fx-cursor: hand; -fx-border-color: " + BORDER_CLR
                            + "; -fx-border-width: 1;"));
            chip.setOnMouseClicked(e -> {
                searchField.setText(related.name());
                handleSearch(related.name());
            });
            relatedStocksPane.getChildren().add(chip);
        }

        // Overview key-fact cards in right panel
        overviewSection.getChildren().clear();
        overviewSection.getChildren().add(overviewCard(snap));

        // Recommendation chart
        List<RecommendationTrends> trends = snap.getRecommendationTrends();
        chartArea.getChildren().removeIf(n -> n != recommendationChart || n instanceof javafx.scene.layout.Region);
        chartArea.getChildren().clear();

        if (trends != null && !trends.isEmpty()) {
            javafx.scene.layout.Region newChart = RecommendationTrendsChart.build(trends, snap.getTicker());
            newChart.setVisible(true);
            newChart.setManaged(true);
            chartArea.getChildren().add(newChart);
        }
        recommendationChart.setVisible(false);
        recommendationChart.setManaged(false);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * A Google-Finance-style metrics cell.
     * @param rightBorder when true, draws a 1px right-side divider line
     */
    private VBox metricsCell(String label, String value, boolean rightBorder) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 12px;");

        Label val = new Label(value);
        val.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        val.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        VBox cell = new VBox(4, lbl, val);
        cell.setPadding(new Insets(10, 14, 10, 14));
        // Bottom border always; right border only on left column
        String rw = rightBorder ? "1" : "0";
        cell.setStyle("-fx-border-color: " + DIVIDER + ";"
                + "-fx-border-width: 0 " + rw + " 1 0;");
        return cell;
    }

    /** Small overview card for the right panel. */
    private VBox overviewCard(StockSnapshot snap) {
        Label title = new Label("About " + snap.getTicker());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        title.setStyle("-fx-text-fill: " + TEXT_PRI + ";");

        VBox card = new VBox(8, title);
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 14px;");

        String[][] items = {
                { "Industry", snap.getStock().getIndustry() },
                { "Country", snap.getCountry() },
                { "P/E Ratio", fmt2(snap.getPeRatio()) },
                { "Price/Book", fmt2(snap.getPriceToBook()) },
                { "Div. Yield", fmt4(snap.getDividendYield()) },
                { "52W High", "$" + fmt2(snap.getWeekHigh52()) },
                { "52W Low", "$" + fmt2(snap.getWeekLow52()) }
        };

        for (String[] item : items) {
            HBox row = new HBox();
            Label k = new Label(item[0]);
            k.setStyle("-fx-text-fill: " + TEXT_SEC + "; -fx-font-size: 12px;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label v = new Label(item[1]);
            v.setStyle("-fx-text-fill: " + TEXT_PRI + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            row.getChildren().addAll(k, sp, v);
            card.getChildren().add(row);
        }
        return card;
    }

    private String fmt2(double v) {
        return String.format("%.2f", v);
    }

    private String fmt4(double v) {
        return String.format("%.4f", v);
    }
}
