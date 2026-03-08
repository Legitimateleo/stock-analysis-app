package team2.parallax.model;

import team2.parallax.data.Fortune500;
import java.util.List;

public class StockSnapshot {
    private final Fortune500 stock;
    private final String companyName;
    private final double currentPrice;
    private final String ticker;
    private final String country;
    private final String logo;
    private final double peRatio;
    private final double priceToBook;
    private final double dividendYield;
    private final double weekHigh52;
    private final double weekLow52;
    private final List<Fortune500> relatedStocks;
    private final List<RecommendationTrends>  recommendationTrends;

    public StockSnapshot(Fortune500 stock, String companyName, double currentPrice,
                         String ticker, String country, String logo, double peRatio,
                         double priceToBook, double dividendYield,
                         double weekHigh52, double weekLow52, List<Fortune500> relatedStocks,
                         List<RecommendationTrends> recommendationTrends) {

        this.stock = stock;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.ticker = ticker;
        this.country = country;
        this.logo = logo;
        this.peRatio = peRatio;
        this.priceToBook = priceToBook;
        this.dividendYield = dividendYield;
        this.weekHigh52 = weekHigh52;
        this.weekLow52 = weekLow52;
        this.relatedStocks = relatedStocks;
        this.recommendationTrends = recommendationTrends;
    }

    public Fortune500 getStock()       { return stock; }
    public String getCompanyName()     { return companyName; }
    public double getCurrentPrice()    { return currentPrice; }
    public String getTicker()          { return ticker; }
    public String getCountry()         { return country; }
    public String getLogo()            { return logo; }
    public double getPeRatio()         { return peRatio; }
    public double getPriceToBook()     { return priceToBook; }
    public double getDividendYield()   { return dividendYield; }
    public double getWeekHigh52()      { return weekHigh52; }
    public double getWeekLow52()       { return weekLow52; }
    public List<Fortune500> getRelatedStocks() { return relatedStocks; }
    public List<RecommendationTrends> getRecommendationTrends() {return recommendationTrends;}

    @Override
    public String toString() {
        return String.format("StockSnapshot{ticker=%s, company=%s, price=%.2f}",
                ticker, companyName, currentPrice);
    }
}