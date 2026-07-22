package team2.parallax.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import team2.parallax.api.DataAccessClient;
import team2.parallax.api.FinnhubClient;
import team2.parallax.api.PolygonClient;
import team2.parallax.backend.service.ChartDataService;
import team2.parallax.service.MarketDataService;

/**
 * Wires MarketDataService for the REST layer.
 *
 * <p>API keys are read exclusively from environment variables
 * (FINNHUB_API_KEY / POLYGON_API_KEY) — never from config.properties.
 * config.properties is a desktop-only artifact and must never be baked
 * into a container image (see .gitignore in the desktop module).</p>
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${FINNHUB_API_KEY:}")
    private String finnhubApiKey;

    @Value("${POLYGON_API_KEY:}")
    private String polygonApiKey;

    @Value("${parallax.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public DataAccessClient dataAccessClient() {
        if (finnhubApiKey == null || finnhubApiKey.isBlank()) {
            throw new IllegalStateException(
                "FINNHUB_API_KEY environment variable is not set. " +
                "Set it in your shell, docker-compose.yml, or cloud provider's " +
                "secret manager before starting the backend."
            );
        }
        return new FinnhubClient(finnhubApiKey);
    }

    @Bean
    public MarketDataService marketDataService(DataAccessClient dataAccessClient) {
        return new MarketDataService(dataAccessClient);
    }

    /**
     * PolygonClient is optional at startup — unlike Finnhub, chart data is
     * a newly in-scope feature (see ChartDataService javadoc) and the app
     * should still boot without it. {@code hasKey()} returns false if the
     * key is blank, and ChartDataService/ChartDataController degrade to an
     * empty result rather than crashing.
     */
    @Bean
    public PolygonClient polygonClient() {
        return new PolygonClient(polygonApiKey);
    }

    @Bean
    public ChartDataService chartDataService(PolygonClient polygonClient) {
        return new ChartDataService(polygonClient);
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET");
    }
}
