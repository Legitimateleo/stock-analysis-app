package team2.parallax.service;
import team2.parallax.data.Fortune500;
import team2.parallax.model.StockSnapshot;
import team2.parallax.data.SectorPE;
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

    public double getSectorPEScore(Fortune500 stock, StockSnapshot snapshot){
        return calculator.sectorPEScore(stock, snapshot);

    }

    //FINAL SCORE!
    public double getFinalScore(Fortune500 stock, StockSnapshot snapshot){
        double peScore = getPEScore(snapshot);
        double highLowScore = getHighLowScore(snapshot);
        double sectorPEScore = getSectorPEScore(stock, snapshot);
        //averages the score (will add two more methods)
        return (peScore + highLowScore + sectorPEScore)/3.0;
    }

    //signal
    public String getSignal(Fortune500 stock, StockSnapshot snapshot){
        double score = getFinalScore(stock, snapshot);

    }

    // ── Full summary ──────────────────────────────────────────────
    public void printSummary(Fortune500 stock, StockSnapshot snapshot) {
        System.out.println("\n── Valuation Score Summary ──────────────");
        System.out.printf("P/E Score:        %.2f%n", getPEScore(snapshot));
        System.out.printf("High/Low Score:   %.2f%n", getHighLowScore(snapshot));
        System.out.printf("Sector P/E Score: %.2f%n", getSectorPEScore(stock,snapshot));
        System.out.println("─────────────────────────────────────────");
        System.out.printf("Final Score:      %.2f / 10%n", getFinalScore(stock,snapshot));
        System.out.println("Signal:           " + getSignal(stock,snapshot));
        System.out.println("─────────────────────────────────────────");
    }
}
