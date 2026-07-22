package team2.parallax.model;

/**
 * ValuationResult is an immutable Data Transfer Object (DTO) that carries
 * the composite valuation score and derived buy/sell signal produced by
 * {@code ValidationScore}.
 *
 * <p>Returned by {@code MarketDataService.getValuation()} to both the
 * JavaFX desktop UI and the Spring Boot REST API, so both consumers see
 * the exact same valuation result shape.</p>
 *
 * @see team2.parallax.service.ValidationScore
 * @see team2.parallax.service.MarketDataService
 */
public class ValuationResult {
    private final double score;
    private final String signal;

    public ValuationResult(double score, String signal) {
        this.score = score;
        this.signal = signal;
    }

    public double getScore()  { return score; }
    public String getSignal() { return signal; }
}
