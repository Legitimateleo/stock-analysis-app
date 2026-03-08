package team2.parallax.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import team2.parallax.api.FinnhubClient;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.data.Fortune500;
import team2.parallax.model.StockSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;


public class MarketDataService {
    //calls client class to connect the methods here to the endpoints
    private final FinnhubClient client;
    //method that converts java objects into readable Json packages
    private final Gson gson = new Gson();

    public MarketDataService(FinnhubClient client) {
        this.client = client;
    }

    public JsonObject getQuote(String symbol) {
        return client.get("quote?symbol=" + symbol);
    }

    public JsonObject getCompanyProfile(String symbol) {
        return client.get("stock/profile2?symbol=" + symbol);
    }

    public JsonObject getFinancialMetrics(String symbol) {
        return client.get("stock/metric?symbol=" + symbol);
    }

    public JsonArray getRecommendationTrends(String symbol) {
        String raw = client.getRaw("stock/recommendation?symbol=" + symbol);
        if (raw == null) return null;
        return gson.fromJson(raw, JsonArray.class);
    }

    public JsonObject getCandles(String symbol, long from, long to) {
        return client.get("stock/candle?symbol=" + symbol
                + "&from=" + from + "&to=" + to);
    }

    public JsonArray getCompanyNews(String symbol, String from, String to) {
        String raw = client.getRaw("company-news?symbol=" + symbol
                + "&from=" + from + "&to=" + to);
        if (raw == null) return null;
        return gson.fromJson(raw, JsonArray.class);
    }

    public JsonObject getInsiderSentiment(String symbol, String from, String to) {
        return client.get("stock/insider-sentiment?symbol=" + symbol
                + "&from=" + from + "&to=" + to);
    }

    public JsonObject getInsiderTransactions(String symbol){
        return client.get("stock/insider-transactions?symbol=" + symbol);
    }

    public Fortune500 search(String input) {
        String normalized = input.trim().toUpperCase();

        for (Fortune500 stock : Fortune500.values()) {
            if(stock.name().equals(normalized)) {
                return stock;
            }
            //
            if(stock.getCompanyName().toUpperCase().contains(normalized)) {
                return stock;
            }
        }
        return null;
    }

    public List<Fortune500> getByIndustry(Fortune500 stock) {
        List<Fortune500> related = new ArrayList<>();
        for (Fortune500 s :  Fortune500.values()) {
            if(s != stock && s.getIndustry().equals(stock.getIndustry())) {
                related.add(s);
            }
        }
        return related;
    }

    private List<RecommendationTrends> getTrends(Fortune500 stock) {
        List<RecommendationTrends> trends = new ArrayList<>();
        JsonArray trendsData = getRecommendationTrends(stock.name());
        if(trendsData == null) return trends;

        for (JsonElement elem : trendsData) {
            JsonObject t = elem.getAsJsonObject();
            trends.add(new RecommendationTrends(
                    t.has("buy")        ? t.get("buy").getAsInt()        : 0,
                    t.has("hold")       ? t.get("hold").getAsInt()       : 0,
                    t.has("period")     ? t.get("period").getAsString()  : "N/A",
                    t.has("sell")       ? t.get("sell").getAsInt()       : 0,
                    t.has("strongBuy")  ? t.get("strongBuy").getAsInt()  : 0,
                    t.has("strongSell") ? t.get("strongSell").getAsInt() : 0
            ));
        }
        return trends;
    }


    public StockSnapshot getSnapshot(Fortune500 stock) {
        String symbol = stock.name();

        //calling quote data
        JsonObject quoteData = getQuote(symbol);
        double currentPrice = quoteData != null ? quoteData.get("c").getAsDouble() : 0;

        //Company Profile Data
        JsonObject profileData = getCompanyProfile(symbol);
        String companyName = "N/A", country = "N/A", logo = "N/A", ticker = symbol;
        if (profileData != null) {
            if (profileData.has("name") && !profileData.get("name").isJsonNull())
                companyName = profileData.get("name").getAsString();
            if (profileData.has("country") && !profileData.get("country").isJsonNull())
                country = profileData.get("country").getAsString();
            if (profileData.get("logo") != null && !profileData.get("logo").isJsonNull())
                logo = profileData.get("logo").getAsString();
            if (profileData.get("ticker") != null && !profileData.get("ticker").isJsonNull())
                ticker = profileData.get("ticker").getAsString();
        }

        //metrics Data
        JsonObject metricsData = getFinancialMetrics(symbol);
        double peRatio = 0, priceToBook = 0, dividendYield = 0, weekHigh52 = 0, weekLow52 = 0;
        if (metricsData != null) {
            JsonObject m = metricsData.getAsJsonObject("metric");
            if (m != null) {
                peRatio = getMetricValue(m, "peBasicExclExtraTTM");
                priceToBook = getMetricValue(m, "pbAnnual");
                dividendYield = getMetricValue(m, "currentDividendYieldTTM");
                weekHigh52 = getMetricValue(m, "52WeekHigh");
                weekLow52 = getMetricValue(m, "52WeekLow");
            }
        }
        List<Fortune500> relatedStocks = getByIndustry(stock);
        List<RecommendationTrends> trends = getTrends(stock);
        return new StockSnapshot(stock, companyName, currentPrice, ticker,
                country, logo, peRatio, priceToBook, dividendYield, weekHigh52, weekLow52, relatedStocks, trends);
    }

    public StockSnapshot lookup(String input) {
        Fortune500 stock = search(input);
        if (stock == null) return null;
        return getSnapshot(stock);
    }

    private double getMetricValue(JsonObject metrics, String key) {
        if (metrics.has(key) && !metrics.get(key).isJsonNull()) {
            return metrics.get(key).getAsDouble();
        }
        return 0;
    }
}





