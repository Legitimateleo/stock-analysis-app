package team2.parallax.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.service.MarketDataProvider;
import team2.parallax.service.ValidationScore;

import java.util.List;

public class ParallaxController {
    private final MarketDataProvider marketData;
    private final ViewCallBack view;

    public MarketDataProvider getMarketData() { return marketData; }

    private Fortune500 currentStock = null;
    private StockSnapshot currentStockSnapshot = null;

    public ParallaxController(MarketDataProvider marketData, ViewCallBack view) {
        this.marketData = marketData;
        this.view = view;
    }

    //getters
    public Fortune500 getCurrentStock() { return currentStock; }
    public StockSnapshot getCurrentStockSnapshot() { return currentStockSnapshot; }

    //Search
    public void handleSearch(String input){
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
            }
        });

        task.setOnFailed(e -> view.onSearchFailure("Search failed. Please try again."
                ));

                new Thread(task).start();
    }

    //Trends
    public void handleTrends() {
        if(currentStock == null) return;

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
            }else {
                view.onTrendsLoadFailure("No trends data available");
            }
        });

        task.setOnFailed(e -> view.onTrendsLoadFailure("Failed to load Trends."));
        new Thread(task).start();

    }
    //Valuation
    public void handleCalculate(){
        if(currentStock == null || currentStockSnapshot == null){
            view.onScoreCalculatedFailure("Please search for a stock first.");
            return;
        }

        ValidationScore valuation = marketData.getValuation(currentStock, currentStockSnapshot);
        double score = valuation.getFinalScore(currentStock, currentStockSnapshot);
        String signal = valuation.getSignal(currentStock, currentStockSnapshot);

        view.onScoreCalculated(score, signal);


    }

    public void handleChartLoad(String ticker) {
        // MainWindow calls this, StockChartPanel handles its own threading
        Platform.runLater(() -> view.onChartLoad(ticker));
    }

}
