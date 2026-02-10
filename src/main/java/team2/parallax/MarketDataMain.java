package team2.parallax;

import team2.parallax.api.MassiveClientWrapper;

public class MarketDataMain {
    public static void main(String[] args) {
        try {
            MassiveClientWrapper wrapper = new MassiveClientWrapper();

            System.out.println("=== S&P 500 Market Price & Name Request ===");

            // Fetch the raw JSON string from the new method
            String rawJson = wrapper.getStockPriceData();

            System.out.println("\n--- RAW MARKET DATA ---");
            System.out.println(rawJson);
            System.out.println("-----------------------");

        } catch (Exception e) {
            System.err.println("Error fetching market data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}