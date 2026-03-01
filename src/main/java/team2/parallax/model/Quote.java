package team2.parallax.model;

public class Quote {
    private final double currentPrice;
    private final double change;
    private final double changePercent;
    private final double highPrice;
    private final double lowPrice;
    private final double openPrice;
    private final double previousClose;

    public Quote(double currentPrice, double change, double changePercent,
                 double highPrice, double lowPrice, double openPrice, double previousClose) {
        this.currentPrice = currentPrice;
        this.change = change;
        this.changePercent = changePercent;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.openPrice = openPrice;
        this.previousClose = previousClose;
    }

    public double getCurrentPrice()  { return currentPrice; }
    public double getChange()        { return change; }
    public double getChangePercent() { return changePercent; }
    public double getHighPrice()     { return highPrice; }
    public double getLowPrice()      { return lowPrice; }
    public double getOpenPrice()     { return openPrice; }
    public double getPreviousClose() { return previousClose; }

    @Override
    public String toString() {
        return String.format("Quote{price=%.2f, change=%.2f (%.2f%%)}",
                currentPrice, change, changePercent);
    }
}

