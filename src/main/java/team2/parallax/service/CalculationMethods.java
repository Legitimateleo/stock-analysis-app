package team2.parallax.service;

import team2.parallax.model.StockSnapshot;
import team2.parallax.data.SectorPE;
import team2.parallax.data.Fortune500;

/**
 * CalculationMethods is a stateless scoring engine that implements the three
 * independent financial scoring methods used by the Parallax valuation engine.
 *
 * <p>Each method receives a {@link StockSnapshot} and optional supporting data,
 * evaluates a specific financial metric against a predefined scoring bracket,
 * and returns a score between 1.0 and 10.0. A higher score indicates a more
 * attractive valuation signal for that particular method.</p>
 *
 * <p>The three methods are designed to be independent of one another and are
 * aggregated externally by {@link ValidationScore}, which averages them into
 * a final composite score and derives the buy/sell signal. This separation
 * ensures each scoring method can be tuned, replaced, or extended without
 * affecting the others.</p>
 *
 * <p>All three methods return a neutral score of {@code 5.0} when the required
 * data is unavailable (e.g. P/E ratio of 0), ensuring the valuation engine
 * degrades gracefully rather than producing misleading results.</p>
 *
 * <p>This class makes zero API calls. All inputs are sourced from the
 * previously fetched {@link StockSnapshot} and the local {@link SectorPE}
 * enum, making the entire calculation instantaneous.</p>
 *
 * @see ValidationScore
 * @see StockSnapshot
 * @see SectorPE
 */

public class CalculationMethods {

    public double forwardPEScore(StockSnapshot snapshot) {
        double pe = snapshot.getPeRatio();

        // P/E of 0 means data unavailable
        if (pe <= 0)
            return 5.0;

        // edited pe's based on avg sectors
        if (pe < 8)
            return 10.0;
        else if (pe < 14)
            return 9.0;
        else if (pe < 20)
            return 8.0;
        else if (pe < 24)
            return 7.0;
        else if (pe < 30)
            return 6.0;
        else if (pe < 60)
            return 4.0;
        else if (pe < 100)
            return 3.0;
        else if (pe < 200)
            return 2.0;
        else
            return 1.0;
    }

    public double highLowScore(StockSnapshot snapshot) {
        double current = snapshot.getCurrentPrice();
        double high = snapshot.getWeekHigh52();
        double low = snapshot.getWeekLow52();

        if (high == low)
            return 5.0;

        // calculate mean
        double mean = (high + low) / 2.0;

        // how far is current price from mean as a percentage
        double deviation = (current - mean) / mean;

        // negative deviation = below mean (potentially undervalued)
        // positive deviation = above mean (potentially overvalued
        if (deviation < -0.30)
            return 10.0;
        else if (deviation < -0.10)
            return 8.0;
        else if (deviation <= 0.10)
            return 6.0;
        else if (deviation <= 0.30)
            return 4.0;
        else
            return 2.0;
    }

    public double sectorPEScore(String industry, StockSnapshot snapshot) {
        double stockPE = snapshot.getPeRatio();
        double sectorPE = SectorPE.getAverageForIndustry(industry);

        if (stockPE <= 0 || sectorPE <= 0)
            return 5.0;

        double ratio = stockPE / sectorPE;

        if (ratio < 0.5)
            return 10.0; // huge discount vs sector
        else if (ratio < 0.75)
            return 8.0; // undervalued vs sector
        else if (ratio < 0.90)
            return 7.0;
        else if (ratio < 1.10)
            return 5.0;
        else if (ratio < 1.25)
            return 3.0; // slightly overvalued
        else if (ratio < 1.50)
            return 2.0; // overvalued
        else
            return 1.0;
    }

}
