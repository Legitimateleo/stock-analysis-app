package team2.parallax.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team2.parallax.backend.dto.ErrorResponse;
import team2.parallax.backend.dto.StockDto;
import team2.parallax.data.Fortune500;
import team2.parallax.model.RecommendationTrends;
import team2.parallax.model.StockSnapshot;
import team2.parallax.model.ValuationResult;
import team2.parallax.service.MarketDataService;

import java.util.List;
import java.util.Map;

/**
 * REST wrapper around {@link MarketDataService}, matching the endpoint
 * table in section 4 of Parallax_Cloud_Plan_of_Attack.md exactly:
 *
 * <pre>
 * GET /api/health             -&gt; service status
 * GET /api/search?input=      -&gt; search()       -&gt; ticker, company, industry
 * GET /api/snapshot?ticker=   -&gt; getSnapshot()   -&gt; full stock metrics
 * GET /api/trends?ticker=     -&gt; getTrends()     -&gt; monthly analyst counts
 * GET /api/valuation?ticker=  -&gt; getValuation()  -&gt; score + signal
 * GET /api/related?ticker=    -&gt; getByIndustry() -&gt; industry peer list
 * </pre>
 *
 * <p>No business logic lives here — every method translates an HTTP
 * request into a facade call and a facade result into JSON. The
 * Streamlit dashboard (Track B) is a second consumer of the same
 * {@code MarketDataService} facade the JavaFX desktop app already uses,
 * just over HTTP instead of a direct Java call.</p>
 */
@RestController
@RequestMapping("/api")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /** GET /api/health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /** GET /api/search?input=nvidia  (ticker or company-name fragment) */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("input") String input) {
        Fortune500 stock = marketDataService.search(input);
        if (stock == null) {
            return notFound(input);
        }
        return ResponseEntity.ok(StockDto.from(stock));
    }

    /** GET /api/snapshot?ticker=NVDA */
    @GetMapping("/snapshot")
    public ResponseEntity<?> getSnapshot(@RequestParam("ticker") String ticker) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return notFound(ticker);
        }
        StockSnapshot snapshot = marketDataService.getSnapshot(stock);
        return ResponseEntity.ok(snapshot);
    }

    /** GET /api/trends?ticker=NVDA */
    @GetMapping("/trends")
    public ResponseEntity<?> getTrends(@RequestParam("ticker") String ticker) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return notFound(ticker);
        }
        List<RecommendationTrends> trends = marketDataService.getTrends(stock);
        return ResponseEntity.ok(trends);
    }

    /**
     * GET /api/valuation?ticker=NVDA
     *
     * <p>Fetches a fresh {@link StockSnapshot} internally before scoring —
     * the client only ever needs the ticker, matching the Plan of Attack's
     * signature ({@code getValuation()} taking just the ticker over HTTP,
     * even though the underlying facade method also needs a snapshot).</p>
     */
    @GetMapping("/valuation")
    public ResponseEntity<?> getValuation(@RequestParam("ticker") String ticker) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return notFound(ticker);
        }
        StockSnapshot snapshot = marketDataService.getSnapshot(stock);
        ValuationResult valuation = marketDataService.getValuation(stock, snapshot);
        return ResponseEntity.ok(valuation);
    }

    /** GET /api/related?ticker=NVDA */
    @GetMapping("/related")
    public ResponseEntity<?> getRelated(@RequestParam("ticker") String ticker) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return notFound(ticker);
        }
        List<StockDto> related = marketDataService.getByIndustry(stock).stream()
                .map(StockDto::from)
                .toList();
        return ResponseEntity.ok(related);
    }

    /**
     * GET /api/logo?ticker=NVDA
     *
     * <p>Not in the Plan of Attack's endpoint table, but
     * {@code getLogoUrl()} is part of {@code MarketDataProvider} and the
     * desktop UI displays the logo — kept available in case Track B wants
     * it for the dashboard header. Safe to ignore or remove if unused.</p>
     */
    @GetMapping("/logo")
    public ResponseEntity<?> getLogo(@RequestParam("ticker") String ticker) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return notFound(ticker);
        }
        String logo = marketDataService.getLogoUrl(stock.name());
        return ResponseEntity.ok(Map.of("ticker", stock.name(), "logo", logo));
    }

    private ResponseEntity<ErrorResponse> notFound(String query) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No Fortune 500 match for \"" + query + "\""));
    }
}
