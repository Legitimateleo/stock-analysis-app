package team2.parallax.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team2.parallax.backend.dto.ErrorResponse;
import team2.parallax.backend.service.ChartDataService;
import team2.parallax.backend.service.ChartRange;
import team2.parallax.data.Fortune500;
import team2.parallax.model.ChartPoint;
import team2.parallax.service.MarketDataService;

import java.util.List;

/**
 * GET /api/chart?ticker=&range=  — use case 5: user changes the
 * historical price graph's time range (1D/1W/1M/3M/1Y/5Y).
 *
 * <p>NOT in the original Plan of Attack's endpoint table — D4 flagged
 * Polygon chart data as out of scope for the cloud deployment pending
 * team sign-off. Confirm D4 before Track B builds a chart page against
 * this endpoint.</p>
 */
@RestController
@RequestMapping("/api")
public class ChartDataController {

    private final MarketDataService marketDataService;
    private final ChartDataService chartDataService;

    public ChartDataController(MarketDataService marketDataService,
                                ChartDataService chartDataService) {
        this.marketDataService = marketDataService;
        this.chartDataService = chartDataService;
    }

    @GetMapping("/chart")
    public ResponseEntity<?> getChart(@RequestParam("ticker") String ticker,
                                       @RequestParam("range") String range) {
        Fortune500 stock = marketDataService.search(ticker);
        if (stock == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No Fortune 500 match for \"" + ticker + "\""));
        }

        ChartRange parsedRange = ChartRange.fromCode(range);
        if (parsedRange == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                        "Invalid range \"" + range + "\". Expected one of: 1D, 1W, 1M, 3M, 1Y, 5Y."
                    ));
        }

        List<ChartPoint> bars = chartDataService.getBars(stock.name(), parsedRange);
        return ResponseEntity.ok(bars);
    }
}
