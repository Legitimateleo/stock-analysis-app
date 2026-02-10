package team2.parallax.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MassiveClientWrapper {
    private final OkHttpClient client;
    private final String apiKey;

    public MassiveClientWrapper() {
        this.client = new OkHttpClient();
        this.apiKey = loadApiKey();
    }

    /**
     * Reads the API key from src/main/resources/config.properties
     */
    private String loadApiKey() {
        Properties prop = new Properties();
        // getClass().getClassLoader() looks inside the 'resources' folder automatically
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties in resources folder");
            }
            prop.load(input);
            return prop.getProperty("api.key"); // Ensure this matches the name inside the file
        } catch (IOException ex) {
            throw new RuntimeException("Error reading config.properties", ex);
        }
    }

    /**
     * Fetches dividend data from the Massive API
     */
    public String getDividendData() throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key is missing. Check config.properties.");
        }

        // Check Rate Limiter before making the call
        RateLimiter.waitForNext();

        // Build the URL (Note: Massive API usually requires the key in the URL or Header)
        String url = "https://api.massive.com/v3/reference/dividends?apiKey=" + apiKey;

        // Prepare the Request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // 4. Execute the call
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code() + " - " + response.message());
            }
            return response.body().string();
        }
    }

    /**
     * Fetches Ticker data from the Massive API
     */
    public String getStockPriceData() throws IOException {
        RateLimiter.waitForNext();

        // This endpoint typically returns tickers, names, and last known prices
        String url = "https://api.massive.com/v3/reference/tickers?active=true&limit=100&apiKey=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("API Error: " + response.code());
            return response.body().string();
        }
    }
}