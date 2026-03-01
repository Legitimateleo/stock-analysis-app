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

    public JsonObject getMarketData(String symbol) {
        return client.get("/quote?symbol=" + symbol);
    }

    public JsonObject getCompanyProfile(String symbol) {
        return client.get("/company/profile?symbol=" + symbol);
    }

    public JsonObject getFinancialMetrics(String symbol) {
        return client.get("/stock/metric?symbol=" + symbol);
    }

    public JsonObject getCandles(String symbol) {
        return client.get("/stock/candle?symbol=" + symbol);
    }

    public JsonObject getCompanyNews(String symbol, String from, String to) {
        return client.get("/stock/company-news?symbol=" + symbol
                + "&from=" + from + "&to=" + to);
    }

    public JsonObject getInsiderSentiment(String symbol, String from, String to) {
        return client.get("/stock/insider-sentiment?symbol=" + symbol
                + "&from=" + from + "&to=" + to);
    }

    public JsonObject getInsiderTransactions(String symbol){
        return client.get("/stock/insider-transactions?symbol=" + symbol);
    }
}


