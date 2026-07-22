package team2.parallax.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Parallax cloud backend.
 *
 * <p>This module exposes the existing {@code MarketDataService} facade
 * (unchanged, copied verbatim from the JavaFX desktop app) as a REST API
 * consumed by the Streamlit dashboard (Track B). The JavaFX application
 * is not modified by, or dependent on, this module in any way.</p>
 */
@SpringBootApplication
public class ParallaxBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParallaxBackendApplication.class, args);
    }
}
