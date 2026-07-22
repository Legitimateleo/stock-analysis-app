package team2.parallax.model;

/**
 * ChartPoint is an immutable Data Transfer Object (DTO) representing a
 * single OHLCV (open/high/low/close/volume) bar from the Polygon.io
 * aggregates endpoint.
 *
 * <p>Returned in lists by {@code ChartDataService.getBars()}. Keeping this
 * as a clean DTO — rather than passing Polygon's raw JSON out of the
 * service layer — preserves the same Facade boundary rule
 * {@code MarketDataService} already follows: no {@code JsonObject} crosses
 * out of the service layer.</p>
 *
 * @see team2.parallax.backend.service.ChartDataService
 */
public class ChartPoint {
    private final long timestamp; // epoch millis, from Polygon's "t" field
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;

    public ChartPoint(long timestamp, double open, double high, double low,
                       double close, long volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public long getTimestamp() { return timestamp; }
    public double getOpen()    { return open; }
    public double getHigh()    { return high; }
    public double getLow()     { return low; }
    public double getClose()   { return close; }
    public long getVolume()    { return volume; }
}
