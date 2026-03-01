package team2.parallax.model;

public class FinancialMetrics {
    // Valuation
    private final double peRatio;
    private final double priceToBook;
    private final double priceToSales;
    private final double eps;
    private final double dividendYield;

    // Profitability
    private final double returnOnEquity;
    private final double returnOnAssets;
    private final double grossMargin;
    private final double operatingMargin;
    private final double netProfitMargin;

    // Financial Health
    private final double currentRatio;
    private final double debtToEquity;
    private final double freeCashFlowPerShare;
    private final double revenuePerShare;

    // Price Performance
    private final double weekHigh52;
    private final double weekLow52;
    private final double beta;

    public FinancialMetrics(double peRatio, double priceToBook, double priceToSales,
                            double eps, double dividendYield, double returnOnEquity,
                            double returnOnAssets, double grossMargin, double operatingMargin,
                            double netProfitMargin, double currentRatio, double debtToEquity,
                            double freeCashFlowPerShare, double revenuePerShare,
                            double weekHigh52, double weekLow52, double beta) {
        this.peRatio = peRatio;
        this.priceToBook = priceToBook;
        this.priceToSales = priceToSales;
        this.eps = eps;
        this.dividendYield = dividendYield;
        this.returnOnEquity = returnOnEquity;
        this.returnOnAssets = returnOnAssets;
        this.grossMargin = grossMargin;
        this.operatingMargin = operatingMargin;
        this.netProfitMargin = netProfitMargin;
        this.currentRatio = currentRatio;
        this.debtToEquity = debtToEquity;
        this.freeCashFlowPerShare = freeCashFlowPerShare;
        this.revenuePerShare = revenuePerShare;
        this.weekHigh52 = weekHigh52;
        this.weekLow52 = weekLow52;
        this.beta = beta;
    }

    // Valuation
    public double getPeRatio()            { return peRatio; }
    public double getPriceToBook()        { return priceToBook; }
    public double getPriceToSales()       { return priceToSales; }
    public double getEps()                { return eps; }
    public double getDividendYield()      { return dividendYield; }

    // Profitability
    public double getReturnOnEquity()     { return returnOnEquity; }
    public double getReturnOnAssets()     { return returnOnAssets; }
    public double getGrossMargin()        { return grossMargin; }
    public double getOperatingMargin()    { return operatingMargin; }
    public double getNetProfitMargin()    { return netProfitMargin; }

    // Financial Health
    public double getCurrentRatio()       { return currentRatio; }
    public double getDebtToEquity()       { return debtToEquity; }
    public double getFreeCashFlowPerShare() { return freeCashFlowPerShare; }
    public double getRevenuePerShare()    { return revenuePerShare; }

    // Price Performance
    public double getWeekHigh52()         { return weekHigh52; }
    public double getWeekLow52()          { return weekLow52; }
    public double getBeta()               { return beta; }

    @Override
    public String toString() {
        return String.format("FinancialMetrics{pe=%.2f, pb=%.2f, eps=%.2f, beta=%.2f}",
                peRatio, priceToBook, eps, beta);
    }
}
