package team2.parallax.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.CalculationMethods;
import team2.parallax.service.MarketDataProvider;
import team2.parallax.service.ValidationScore;

import java.util.List;

/**
 * ParallaxController is the MVC Controller of the Parallax system.
 * It mediates between the UI layer ({@link MainWindow}) and the Service
 * layer ({@link MarketDataProvider}), handling all user-triggered actions,
 * managing application state, and coordinating background threading via
 * JavaFX {@link Task}.
 *
 * <p>The controller holds the two critical state fields that persist between
 * user interactions: {@code currentStock} and {@code currentStockSnapshot}.
 * These are set on a successful search and consumed by subsequent operations
 * such as Calculate Valuation and Show Trends.</p>
 *
 * <p>All results are communicated back to the view exclusively through the
 * {@link ViewCallBack} interface. The controller never imports or references
 * any JavaFX UI component directly, maintaining a clean MVC separation.</p>
 *
 * <p>The controller depends on {@link MarketDataProvider} rather than the
 * concrete {@code MarketDataService}, ensuring the service layer can be
 * substituted without modifying the controller.</p>
 *
 * @see ViewCallBack
 * @see MarketDataProvider
 * @see MainWindow
 */

public class ParallaxController {
    /**
     * The service layer facade providing all market data operations.
     * Declared as {@link MarketDataProvider} interface to enforce the
     * UI/Service layer boundary contract.
     */
    private final MarketDataProvider marketData;
    /**
     * The view callback interface used to communicate all results and
     * failures back to {@link MainWindow}. The controller never holds a
     * direct reference to MainWindow — only to this interface.
     */
    private final ViewCallBack view;
    /**
     * The Fortune500 enum constant representing the most recently
     * searched stock. Null until a successful search is completed.
     * Retained for use by {@link #handleTrends()},
     * {@link #handleCalculate()}, and {@link #handleRelatedStocks(Fortune500)}.
     */
    private Fortune500 currentStock = null;
    /**
     * The StockSnapshot fetched during the most recent successful search.
     * Null until a successful search is completed. Passed to the valuation
     * engine by {@link #handleCalculate()}.
     */
    private StockSnapshot currentStockSnapshot = null;
    /**
     * Constructs a ParallaxController wired to the given service layer
     * and view callback.
     *
     * @param marketData the {@link MarketDataProvider} implementation
     *                   providing all market data operations.
     * @param view       the {@link ViewCallBack} implementation that will
     *                   receive all results and failure notifications.
     */
    public ParallaxController(MarketDataProvider marketData, ViewCallBack view) {
        this.marketData = marketData;
        this.view = view;
    }

    /**
     * Returns the StockSnapshot from the most recent successful search.
     *
     * @return the current {@link StockSnapshot}, or {@code null} if no
     *         search has been completed.
     */
    public Fortune500 getCurrentStock() {
        return currentStock;
    }

    /**
     * Returns the StockSnapshot from the most recent successful search.
     *
     * @return the current {@link StockSnapshot}, or {@code null} if no
     *         search has been completed.
     */
    public void handleSearch(String input) {
        if (input == null || input.trim().isEmpty()) return;

        currentStock = null;
        currentStockSnapshot = null;

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
                view.onSearchFailure("No stock found or not in Fortune 500");
            } else {
                currentStockSnapshot = snapshot;
                view.onSearchSuccess(currentStock, snapshot);
                handleRelatedStocks(currentStock);
            }
        });


        task.setOnFailed(e -> view.onSearchFailure("Search failed. Please try again."
        ));

        new Thread(task).start();
    }

    /**
     * Handles the Show Trends button action. Fetches analyst recommendation
     * trends for the current stock on a background thread and notifies
     * the view on completion or failure.
     * Returns early if no stock has been searched yet.
     */
    public void handleTrends() {
        if (currentStock == null) return;

        Task<List<RecommendationTrends>> task = new Task<>() {
            @Override
            protected List<RecommendationTrends> call() {
                return marketData.getTrends(currentStock);
            }
        };

        task.setOnSucceeded(e -> {
            List<RecommendationTrends> trends = task.getValue();
            if (trends != null) {
                view.onTrendsLoaded(trends);
            } else {
                view.onTrendsLoadFailure("No trends data available");
            }
        });

        task.setOnFailed(e -> view.onTrendsLoadFailure("Failed to load Trends."));
        new Thread(task).start();

    }

    /**
     * Handles the Calculate Valuation button action. Instantiates the
     * valuation engine, computes the composite score and signal for the
     * current stock and snapshot, and notifies the view with the result.
     * Returns early with a failure notification if no stock has been searched.
     */
    public void handleCalculate() {
        if (currentStock == null || currentStockSnapshot == null) {
            view.onScoreCalculatedFailure("Please search for a stock first.");
            return;
        }
        String industry = currentStock.getIndustry();
        ValidationScore valuation = new ValidationScore(new CalculationMethods());
        double score = valuation.getFinalScore(industry, currentStockSnapshot);
        String signal = valuation.getSignal(industry, currentStockSnapshot);
        view.onScoreCalculated(score, signal);

    }

    /**
     * Triggers a chart load for the given ticker by posting an
     * {@link #view#onChartLoad(String)} call to the JavaFX Application Thread.
     * StockChartPanel manages its own background threading internally.
     *
     * @param ticker the stock ticker symbol to load the chart for
     *               (e.g. {@code "NVDA"}).
     */
    public void handleChartLoad(String ticker) {
        // MainWindow calls this, StockChartPanel handles its own threading
        Platform.runLater(() -> view.onChartLoad(ticker));
    }

    /**
     * Fetches the list of industry peers for the given stock on a background
     * thread, limits the result to 16 entries for UI performance, notifies
     * the view with the list, then spawns a secondary thread to fetch each
     * peer's logo URL and deliver them to the view incrementally via
     * {@link ViewCallBack#onLogoFetched(int, String, String)}.
     *
     * @param stock the {@link Fortune500} stock whose industry peers
     *              should be fetched and displayed.
     */
    public void handleRelatedStocks(Fortune500 stock) {
        Task<List<Fortune500>> task = new Task<>() {
            //
            @Override
            protected List<Fortune500> call() {
                return marketData.getByIndustry(stock);
            }
        };
        // Cap at 16 related stocks to keep the UI panel manageable.
        task.setOnSucceeded(e -> {
            List<Fortune500> allRelated = task.getValue();
            List<Fortune500> related = allRelated.size() > 16
                    ? allRelated.subList(0, 16)
                    : allRelated;
            view.onRelatedStocksLoaded(related);
            // Secondary thread fetches logos incrementally so the chips
            // render immediately and logos populate as they arrive.
            new Thread(() -> {
                for (int i = 0; i < related.size(); i++) {
                    String ticker = related.get(i).name();
                    String logoUrl = marketData.getLogoUrl(ticker);
                    view.onLogoFetched(i, ticker, logoUrl);
                }
            }).start();
        });

        task.setOnFailed(e -> view.onRelatedStocksLoaded(List.of()));
        new Thread(task).start();
    }
}
