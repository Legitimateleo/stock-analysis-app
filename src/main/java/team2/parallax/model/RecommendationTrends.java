package team2.parallax.model;

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


    @Override
    public String toString() {
        return String.format("RecommendationTrends{period=%s, strongBuy=%d, buy=%d, hold=%d, sell=%d, strongSell=%d}",
                period, strongBuy, buy, hold, sell, strongSell);
    }
}
