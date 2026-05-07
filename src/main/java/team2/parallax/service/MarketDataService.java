package team2.parallax.service;
import team2.parallax.api.DataAccessClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.data.Fortune500;
import team2.parallax.model.StockSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;


public class MarketDataService implements MarketDataProvider {
    //calls client class to connect the methods here to the endpoints
    private final DataAccessClient client;
    //method that converts java objects into readable Json packages
    private final Gson gson = new Gson();

    public MarketDataService(DataAccessClient client) {
        this.client = client;
    }

    public JsonArray getRecommendationTrends(String symbol) {
        String raw = client.getRaw("stock/recommendation?symbol=" + symbol);
        if (raw == null) return null;
        return gson.fromJson(raw, JsonArray.class);
    }


    public Fortune500 search(String input) {
        String normalized = input.trim().toUpperCase();

        for (Fortune500 stock : Fortune500.values()) {
            if (stock.name().equals(normalized)) {
                return stock;
            }
        }

        for (Fortune500 stock : Fortune500.values()) {
            if (stock.getCompanyName().toUpperCase().contains(normalized)) {
                return stock;
            }
        }

        return null;
    }

    public List<Fortune500> getByIndustry(Fortune500 stock) {
        List<Fortune500> related = new ArrayList<>();
        for (Fortune500 s : Fortune500.values()) {
            if (s != stock && s.getIndustry().equals(stock.getIndustry())) {
                related.add(s);
            }
        }
        return related;
    }

    public List<RecommendationTrends> getTrends(Fortune500 stock) {
        List<RecommendationTrends> trends = new ArrayList<>();
        JsonArray trendsData = getRecommendationTrends(stock.name());
        if (trendsData == null) return trends;

        for (JsonElement elem : trendsData) {
            JsonObject t = elem.getAsJsonObject();
            trends.add(new RecommendationTrends(
                    t.has("buy") ? t.get("buy").getAsInt() : 0,
                    t.has("hold") ? t.get("hold").getAsInt() : 0,
                    t.has("period") ? t.get("period").getAsString() : "N/A",
                    t.has("sell") ? t.get("sell").getAsInt() : 0,
                    t.has("strongBuy") ? t.get("strongBuy").getAsInt() : 0,
                    t.has("strongSell") ? t.get("strongSell").getAsInt() : 0
            ));
        }
        return trends;
    }


    public StockSnapshot getSnapshot(Fortune500 stock) {
        String symbol = stock.name();


        JsonObject quoteData = client.get("quote?symbol=" + symbol);
        double currentPrice = 0, change = 0, changePercent = 0;
        if (quoteData != null) {
            currentPrice = getDoubleOrZero(quoteData, "c");
            change = getDoubleOrZero(quoteData, "d");
            changePercent = getDoubleOrZero(quoteData, "dp");
        }

        JsonObject metricsData = client.get("stock/metric?symbol=" + symbol + "&metric=all");
        double peRatio = 0, priceToBook = 0, dividendYield = 0,
                weekHigh52 = 0, weekLow52 = 0, freeCashFlowPerShare = 0;
        double marketCap = 0, eps = 0, revenueYoy = 0;
        if (metricsData != null) {
            JsonObject m = metricsData.getAsJsonObject("metric");
            if (m != null) {
                peRatio = getMetricValue(m, "peBasicExclExtraTTM");
                priceToBook = getMetricValue(m, "pbAnnual");
                dividendYield = getMetricValue(m, "currentDividendYieldTTM");
                weekHigh52 = getMetricValue(m, "52WeekHigh");
                weekLow52 = getMetricValue(m, "52WeekLow");
                freeCashFlowPerShare = getMetricValue(m, "cashFlowPerShareTTM");
                marketCap = getMetricValue(m, "marketCapitalization");
                eps = getMetricValue(m, "epsTTM");
                revenueYoy = getMetricValue(m, "revenueGrowthTTMYoy");
            }
        }

        //call logo for summary
        String logo = getLogoUrl(symbol);

        return new StockSnapshot(currentPrice, change, changePercent,
                peRatio, priceToBook, dividendYield,
                weekHigh52, weekLow52, freeCashFlowPerShare,
                marketCap, eps, revenueYoy, logo);
    }

    //call logo for related stocks
    public String getLogoUrl(String symbol) {
        JsonObject profileData = client.get("stock/profile2?symbol=" + symbol);
        if (profileData != null && profileData.has("logo")
                && !profileData.get("logo").isJsonNull()) {
            return profileData.get("logo").getAsString();
        }
        return "N/A";
    }

    //
    public StockSnapshot lookup(String input) {
        Fortune500 stock = search(input);
        if (stock == null) return null;
        return getSnapshot(stock);
    }

    private double getDoubleOrZero(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0;
    }

    private double getMetricValue(JsonObject metrics, String key) {
        if (metrics.has(key) && !metrics.get(key).isJsonNull()) {
            return metrics.get(key).getAsDouble();
        }
        return 0;
    }
}






