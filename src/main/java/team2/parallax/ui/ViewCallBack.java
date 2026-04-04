package team2.parallax.ui;

import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
 

import java.util.List;

public interface ViewCallBack {
    // Search Results
    void onSearchSuccess(Fortune500 stock, StockSnapshot snapshot);
    void onSearchFailure(String message);

    //trends
    void onTrendsLoaded(List<RecommendationTrends> trends);
    void onTrendsLoadFailure(String message);

    //Valuation
    void onScoreCalculated(double score, String signal);
    void onScoreCalculatedFailure(String message);
}
