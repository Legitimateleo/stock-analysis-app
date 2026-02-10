package team2.parallax;

import team2.parallax.api.MassiveClientWrapper;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Parallax API Connectivity Test ===");

        try {
            // 1. Initialize the wrapper
            MassiveClientWrapper api = new MassiveClientWrapper();

            // 2. Attempt to fetch Dividend data
            System.out.println("Requesting data from Massive...");
            String result = api.getDividendData();

            // 3. Print the raw JSON response
            System.out.println("\n--- API RESPONSE SUCCESS ---");
            System.out.println(result);
            System.out.println("----------------------------");

        } catch (Exception e) {
            System.err.println("\n!!! TEST FAILED !!!");
            System.err.println("Error Message: " + e.getMessage());

            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                System.err.println("Hint: Your API Key might be invalid or not yet active.");
            } else if (e.getMessage().contains("429")) {
                System.err.println("Hint: Rate limit exceeded. Wait 60 seconds and try again.");
            }

            e.printStackTrace();
        }
    }
}