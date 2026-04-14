package team2.parallax.api;

import com.google.gson.JsonObject;

public interface DataAccessClient {
    // Core data access methods
    /**
     * Makes a GET request to the given endpoint and returns
     * the response as a parsed JsonObject.
     * Use for endpoints that return a JSON object.
     */
    JsonObject get(String endpoint);

    /**
     * Makes a GET request to the given endpoint and returns
     * the raw JSON string.
     * Use for endpoints that return a JSON array (e.g. news, trends)
     */
    String getRaw(String endpoint);
}
