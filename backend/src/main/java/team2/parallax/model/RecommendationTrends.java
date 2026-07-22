package team2.parallax.model;

/**
 * RecommendationTrends is an immutable Data Transfer Object (DTO) that
 * carries one month of analyst recommendation consensus data retrieved
 * from the Finnhub {@code /stock/recommendation} endpoint.
 *
 * <p>Each instance represents a single monthly data point containing the
 * aggregate count of analyst recommendations across five sentiment
 * categories: Strong Buy, Buy, Hold, Sell, and Strong Sell. A list of
 * these objects — typically covering the four most recent months — is
 * returned by {@code MarketDataService.getTrends()} and passed to
 * {@code RecommendationTrendsChart.build()} to render the stacked bar
 * chart in the UI.</p>
 *
 * <p>All fields are declared {@code final} to enforce immutability.
 * Each instance represents a fixed point-in-time snapshot of analyst
 * consensus and is never modified after construction.</p>
 *
 * @see team2.parallax.service.MarketDataService
 * @see team2.parallax.ui.RecommendationTrendsChart
 */

public class RecommendationTrends {
    private final int buy;
    private final int hold;
    private final String period;
    private final int sell;
    private final int strongBuy;
    private final int strongSell;

    public RecommendationTrends(int buy, int hold, String period,
                                int sell, int strongBuy, int strongSell)
    {
        this.buy = buy;
        this.hold = hold;
        this.period = period;
        this.sell = sell;
        this.strongBuy = strongBuy;
        this.strongSell = strongSell;
    }

    public int getBuy()       { return buy; }
    public int getHold()     { return hold; }
    public String getPeriod()   { return period; }
    public int getSell()   { return sell; }
    public int getStrongBuy()    { return strongBuy; }
    public int getStrongSell()     { return strongSell; }

}
