package team2.parallax.service;
import team2.parallax.model.StockSnapshot;
public class ValidationScore {
    private final CalculationMethods calculator;

    public ValidationScore(CalculationMethods calculator) {
        this.calculator = calculator;
    }

    // Individual method scores
    public double getPEScore(StockSnapshot snapshot){
        return calculator.forwardPEScore(snapshot);
    }

    public double getHighLowScore(StockSnapshot snapshot){
        return calculator.highLowScore(snapshot);
    }

    public double getSectorPEScore(String industry, StockSnapshot snapshot){
        return calculator.sectorPEScore(industry, snapshot);

    }

    //FINAL SCORE!
    public double getFinalScore(String industry, StockSnapshot snapshot){
        double peScore = getPEScore(snapshot);
        double highLowScore = getHighLowScore(snapshot);
        double sectorPEScore = getSectorPEScore(industry, snapshot);
        //averages the score (will add two more methods)
        return (peScore + highLowScore + sectorPEScore)/3.0;
    }

    //signal
    public String getSignal(String industry, StockSnapshot snapshot){
        double score = getFinalScore(industry, snapshot);

        if (score >= 7.0)      return "STRONG BUY";
        else if (score >= 5.1) return "BUY";
        else if (score == 5.0) return "HOLD";
        else if (score >= 3.0) return "SELL";
        else                   return "STRONG SELL";

    }
}
