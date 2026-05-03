package team2.parallax.model;

public class StockSnapshot {
    private final double currentPrice;
    private final double change;
    private final double changePercent;
    private final double peRatio;
    private final double priceToBook;
    private final double dividendYield;
    private final double weekHigh52;
    private final double weekLow52;
    private final double freeCashFlowPerShare;
    private final double marketCap;
    private final double eps;
    private final double revenueYoy;
    private final String logo;

    public StockSnapshot(double currentPrice, double change, double changePercent,
                         double peRatio, double priceToBook, double dividendYield,
                         double weekHigh52, double weekLow52,
                         double freeCashFlowPerShare,
                         double marketCap, double eps,
                         double revenueYoy, String logo) {
        this.currentPrice         = currentPrice;
        this.change               = change;
        this.changePercent        = changePercent;
        this.peRatio              = peRatio;
        this.priceToBook          = priceToBook;
        this.dividendYield        = dividendYield;
        this.weekHigh52           = weekHigh52;
        this.weekLow52            = weekLow52;
        this.freeCashFlowPerShare = freeCashFlowPerShare;
        this.marketCap            = marketCap;
        this.eps                  = eps;
        this.revenueYoy           = revenueYoy;
        this.logo                 = logo;
    }

    public double getCurrentPrice()          { return currentPrice; }
    public double getChange()                { return change; }
    public double getChangePercent()         { return changePercent; }
    public double getPeRatio()               { return peRatio; }
    public double getPriceToBook()           { return priceToBook; }
    public double getDividendYield()         { return dividendYield; }
    public double getWeekHigh52()            { return weekHigh52; }
    public double getWeekLow52()             { return weekLow52; }
    public double getFreeCashFlowPerShare()  { return freeCashFlowPerShare; }
    public double getMarketCap()             { return marketCap; }
    public double getEps()                   { return eps; }
    public double getRevenueYoy()            { return revenueYoy; }
    public String getLogo()                  { return logo; }

    @Override
    public String toString() {
        return String.format("StockSnapshot{price=%.2f, pe=%.2f, 52wHigh=%.2f, 52wLow=%.2f}",
                currentPrice, peRatio, weekHigh52, weekLow52);
    }
}