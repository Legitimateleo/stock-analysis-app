package team2.parallax.service;
import team2.parallax.model.StockSnapshot;

/**
 * ValidationScore aggregates the three independent scoring methods from
 * {@link CalculationMethods} into a single composite valuation score and
 * derives a human-readable buy/sell signal for display in the Parallax UI.
 *
 * <p>This class acts as the final stage of the valuation pipeline. It owns
 * a {@link CalculationMethods} instance and delegates each individual scoring
 * calculation to it, then averages the results into a composite score between
 * {@code 1.0} and {@code 10.0}. The composite score is then mapped to one of
 * five signal labels: STRONG BUY, BUY, HOLD, SELL, or STRONG SELL.</p>
 *
 * <p>The three scoring methods that contribute to the final score are:</p>
 * <ol>
 *   <li>{@link CalculationMethods#forwardPEScore(StockSnapshot)} —
 *       scores the stock's P/E ratio against market average brackets.</li>
 *   <li>{@link CalculationMethods#highLowScore(StockSnapshot)} —
 *       scores the stock's price deviation from its 52-week mean.</li>
 *   <li>{@link CalculationMethods#sectorPEScore(String, StockSnapshot)} —
 *       scores the stock's P/E relative to its industry sector average.</li>
 * </ol>
 *
 * <p>This class makes zero API calls. All data is sourced from the previously
 * fetched {@link StockSnapshot} and the local {@code SectorPE} enum via
 * {@link CalculationMethods}. The entire calculation is instantaneous.</p>
 *
 * <p>ValidationScore is instantiated by {@code MarketDataService.getValuation()}
 * and returned to {@code ParallaxController}, which calls
 * {@link #getFinalScore(String, StockSnapshot)} and
 * {@link #getSignal(String, StockSnapshot)} to retrieve the results
 * for display in the UI.</p>
 *
 * @see CalculationMethods
 * @see team2.parallax.service.MarketDataService
 * @see team2.parallax.ui.ParallaxController
 */

public class ValidationScore {
    /**
     * The calculation engine providing the three individual scoring methods.
     * Declared as a final field — injected at construction time and never
     * replaced, ensuring the scoring engine is consistent across all method
     * calls on this instance.
     */
    private final CalculationMethods calculator;

    /**
     * Constructs a ValidationScore with the provided {@link CalculationMethods}
     * instance. The calculator is injected rather than instantiated internally
     * to support future interface-based substitution of the scoring engine.
     *
     * @param calculator the {@link CalculationMethods} instance to use for
     *                   all individual score calculations.
     */
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

    /**
     * Computes and returns the final composite valuation score by averaging
     * the three individual method scores.
     *
     * <p>The composite score is calculated as:
     * {@code (peScore + highLowScore + sectorPEScore) / 3.0}</p>
     *
     * <p>Each contributing score is between {@code 1.0} and {@code 10.0},
     * so the composite score is also bounded within that range. A higher
     * score indicates a more attractive valuation signal. This score is
     * displayed numerically in the Parallax UI alongside the signal label
     * returned by {@link #getSignal(String, StockSnapshot)}.</p>
     *
     * <p>Note: The comment in the source indicates two additional scoring
     * methods are planned for a future iteration, which would update the
     * divisor accordingly.</p>
     *
     * @param industry the industry classification string used for the sector
     *                 P/E scoring method. Sourced from {@code Fortune500.getIndustry()}.
     * @param snapshot the {@link StockSnapshot} containing all required
     *                 financial data for scoring.
     * @return the composite valuation score as a double between {@code 1.0}
     *         and {@code 10.0}.
     */
    public double getFinalScore(String industry, StockSnapshot snapshot){
        double peScore = getPEScore(snapshot);
        double highLowScore = getHighLowScore(snapshot);
        double sectorPEScore = getSectorPEScore(industry, snapshot);
        //averages the score (will add two more methods)
        return (peScore + highLowScore + sectorPEScore)/3.0;
    }

    /**
     * Computes and returns the final composite valuation score by averaging
     * the three individual method scores.
     *
     * <p>The composite score is calculated as:
     * {@code (peScore + highLowScore + sectorPEScore) / 3.0}</p>
     *
     * <p>Each contributing score is between {@code 1.0} and {@code 10.0},
     * so the composite score is also bounded within that range. A higher
     * score indicates a more attractive valuation signal. This score is
     * displayed numerically in the Parallax UI alongside the signal label
     * returned by {@link #getSignal(String, StockSnapshot)}.</p>
     *
     * <p>Note: The comment in the source indicates two additional scoring
     * methods are planned for a future iteration, which would update the
     * divisor accordingly.</p>
     *
     * @param industry the industry classification string used for the sector
     *                 P/E scoring method. Sourced from {@code Fortune500.getIndustry()}.
     * @param snapshot the {@link StockSnapshot} containing all required
     *                 financial data for scoring.
     * @return the composite valuation score as a double between {@code 1.0}
     *         and {@code 10.0}.
     */
    public String getSignal(String industry, StockSnapshot snapshot){
        double score = getFinalScore(industry, snapshot);

        if (score >= 7.0)      return "STRONG BUY";
        else if (score >= 5.1) return "BUY";
        else if (score == 5.0) return "HOLD";
        else if (score >= 3.0) return "SELL";
        else                   return "STRONG SELL";

    }
}
