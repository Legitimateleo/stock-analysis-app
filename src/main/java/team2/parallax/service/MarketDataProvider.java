package team2.parallax.service;

import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;

import java.util.List;

public interface MarketDataProvider {
    // ── Core search ───────────────────────────────────────────────
    Fortune500 search(String input);

    // ── Data fetching ─────────────────────────────────────────────
    StockSnapshot getSnapshot(Fortune500 stock);

    List<RecommendationTrends> getTrends(Fortune500 stock);

    // ── Calculations ──────────────────────────────────────────────
    ValidationScore getValuation(Fortune500 stock, StockSnapshot snapshot);

    // ── Industry ──────────────────────────────────────────────────
    List<Fortune500> getByIndustry(Fortune500 stock);

    // ── Extras ────────────────────────────────────────────────────
    String getLogoUrl(String symbol);
}
