package team2.parallax.service;
import team2.parallax.service.CalculationMethods;
import team2.parallax.service.ValidationScore;
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
        for (Fortune500 s :  Fortune500.values()) {
            if(s != stock && s.getIndustry().equals(stock.getIndustry())) {
                related.add(s);
            }
        }
        return related;
    }

    public List<RecommendationTrends> getTrends(Fortune500 stock) {
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

        // ── 1 call: Quote ─────────────────────────────────────────────
        JsonObject quoteData = client.get("quote?symbol=" + symbol);
        double currentPrice = 0, change = 0, changePercent = 0;
        if (quoteData != null) {
            currentPrice  = getDoubleOrZero(quoteData, "c");
            change        = getDoubleOrZero(quoteData, "d");
            changePercent = getDoubleOrZero(quoteData, "dp");
        }

        // ── 2 call: Metrics ───────────────────────────────────────────
        JsonObject metricsData = client.get("stock/metric?symbol=" + symbol + "&metric=all");
        double peRatio = 0, priceToBook = 0, dividendYield = 0,
                weekHigh52 = 0, weekLow52 = 0, freeCashFlowPerShare = 0;
        if (metricsData != null) {
            JsonObject m = metricsData.getAsJsonObject("metric");
            if (m != null) {
                peRatio              = getMetricValue(m, "peBasicExclExtraTTM");
                priceToBook          = getMetricValue(m, "pbAnnual");
                dividendYield        = getMetricValue(m, "currentDividendYieldTTM");
                weekHigh52           = getMetricValue(m, "52WeekHigh");
                weekLow52            = getMetricValue(m, "52WeekLow");
                freeCashFlowPerShare = getMetricValue(m, "cashFlowPerShareTTM");
            }
        }

        // ── 3 call: Logo only ─────────────────────────────────────────
        JsonObject profileData = client.get("stock/profile2?symbol=" + symbol);
        String logo = "N/A";
        if (profileData != null && profileData.has("logo")
                && !profileData.get("logo").isJsonNull()) {
            logo = profileData.get("logo").getAsString();
        }

        return new StockSnapshot(currentPrice, change, changePercent,
                peRatio, priceToBook, dividendYield,
                weekHigh52, weekLow52, freeCashFlowPerShare, logo);
    }

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

    public double getSectorAveragePE(Fortune500 stock){
        List<Fortune500> peers = getByIndustry(stock);

        double total = 0;
        int count = 0;

        for (Fortune500 peer : peers) {
            JsonObject metrics = getFinancialMetrics(peer.name());
            if (metrics == null) continue;

            JsonObject m = metrics.getAsJsonObject("metric");
            if (m == null) continue;

            double pe = getMetricValue(m, "peBasicExclExtraTTM");
            if (pe >0) {
                total += pe;
                count++;
            }
        }
        //returns total sum of PE in the sector and divides it by the number of companies within that sector
        //also ensures count is greater than 0.
        return count > 0 ? total / count : 0;
    }

    public ValidationScore getValuation(Fortune500 stock, StockSnapshot snapshot) {
        return new ValidationScore( new CalculationMethods());
    }
}






