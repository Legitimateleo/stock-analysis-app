package team2.parallax.api;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FinnhubClient {

    private static final String BASE_URL = "https://finnhub.io/api/v1/";
    private static final long MIN_DELAY_MS = 200;

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private long lastRequestTime = 0;


    public FinnhubClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    //returns parsed JSON Object used for endpoints.
    public JsonObject get(String endpoint){
        String raw = getRaw(endpoint);
        if (raw == null) return null;
        try{
            return gson.fromJson(raw, JsonObject.class);
        } catch (Exception e) {
            System.out.println("ERROR parsing JSON: " + e.getMessage());
            return null;
        }
    }

    //Returns raw JSON string used for endpoints that reuturn arrays (COMPANY NEWS)
    public String getRaw(String endpoint) {

        //rate limiting
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastRequestTime;
        if (elapsed < MIN_DELAY_MS) {
            try {
                Thread.sleep(MIN_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();

        //Build URL
        String url = BASE_URL + endpoint
                + (endpoint.contains("?") ? "&" : "?")
                + "token=" + apiKey;


        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();
            if (status == 200) return response.body();
            else if (status == 429) System.out.println("RATE LIMITED -- wait and retry");
            else if (status == 401) System.out.println("UNAUTHORIZED -- check API key");
            else if (status == 403) System.out.println("FORBIDDEN -- may need paid plan");
            else System.out.println("HTTP ERROR " + status + ": " + response.body());

        } catch (IOException e) {
            System.out.println("Connection Error: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("REQUEST INTERRUPTED");
        }
        return null;
    }
}
