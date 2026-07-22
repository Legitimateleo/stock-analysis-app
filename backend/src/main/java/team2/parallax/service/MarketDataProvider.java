package team2.parallax.service;

import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.model.ValuationResult;

import java.util.List;

public interface MarketDataProvider {
    // ── Core search ───────────────────────────────────────────────
    Fortune500 search(String input);

    // ── Data fetching ─────────────────────────────────────────────
    StockSnapshot getSnapshot(Fortune500 stock);

    List<RecommendationTrends> getTrends(Fortune500 stock);

    // ── Valuation ─────────────────────────────────────────────────
    // NOTE: added to close the gap between the Plan of Attack (which
    // already documents this as one of the five facade methods) and the
    // implementation, which previously only had the scoring engine
    // (CalculationMethods/ValidationScore) without a facade entry point.
    ValuationResult getValuation(Fortune500 stock, StockSnapshot snapshot);

    // ── Industry ──────────────────────────────────────────────────
    List<Fortune500> getByIndustry(Fortune500 stock);

    // ── Extras ────────────────────────────────────────────────────
    String getLogoUrl(String symbol);
}
