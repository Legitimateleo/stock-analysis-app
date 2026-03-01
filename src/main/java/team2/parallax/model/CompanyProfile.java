package team2.parallax.model;

public class CompanyProfile {
    private final String name;
    private final String ticker;
    private final String exchange;
    private final String industry;
    private final String country;
    private final String webUrl;
    private final String logo;
    private final double marketCap;
    private final String currency;
    private final String ipo;

    public CompanyProfile(String name, String ticker, String exchange,
                          String industry, String country, String webUrl,
                          String logo, double marketCap, String currency, String ipo) {
        this.name = name;
        this.ticker = ticker;
        this.exchange = exchange;
        this.industry = industry;
        this.country = country;
        this.webUrl = webUrl;
        this.logo = logo;
        this.marketCap = marketCap;
        this.currency = currency;
        this.ipo = ipo;
    }

    public String getName()       { return name; }
    public String getTicker()     { return ticker; }
    public String getExchange()   { return exchange; }
    public String getIndustry()   { return industry; }
    public String getCountry()    { return country; }
    public String getWebUrl()     { return webUrl; }
    public String getLogo()       { return logo; }
    public double getMarketCap()  { return marketCap; }
    public String getCurrency()   { return currency; }
    public String getIpo()        { return ipo; }

    @Override
    public String toString() {
        return String.format("CompanyProfile{name=%s, ticker=%s, industry=%s, marketCap=%.2fM}",
                name, ticker, industry, marketCap);
    }
}
