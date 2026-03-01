package team2.parallax.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import team2.parallax.api.FinnhubClient;


public class MarketDataService {

    private final FinnhubClient client;
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
}


