package team2.parallax.data;

/**
 * SectorPE is an enumeration of industry sectors and their pre-calculated
 * average Price-to-Earnings (P/E) ratios, used by the Parallax valuation
 * engine to contextualize a stock's P/E ratio relative to its industry peers.
 *
 * <p>Each constant represents one industry classification and stores the
 * average P/E ratio for that sector, calculated from live Finnhub data
 * using the {@code SectorPECalculator} utility class at the time of
 * generation. The industry name string must exactly match the industry
 * strings stored in the {@link Fortune500} enum for the lookup in
 * {@link #getAverageForIndustry(String)} to succeed.</p>
 *
 * <p>This enum eliminates the need for any API call during the valuation
 * calculation. Rather than fetching sector averages at runtime, the
 * pre-calculated values are embedded directly in the enum and accessed
 * in O(1) time via the {@link #getAverageForIndustry(String)} lookup.
 * This is a deliberate performance and architectural decision — the
 * sector P/E scoring method requires zero network activity.</p>
 *
 * <p>These values should be regenerated periodically using the
 * {@code SectorPECalculator} utility when market conditions shift
 * significantly, as sector average P/E ratios change over time.</p>
 *
 * @see Fortune500
 * @see team2.parallax.service.CalculationMethods
 */

public enum SectorPE {

    BUILDING("Building", 22.71),
    AEROSPACE_AND_DEFENSE("Aerospace & Defense", 35.52),
    BIOTECHNOLOGY("Biotechnology", 32.86),
    ROAD_AND_RAIL("Road & Rail", 27.22),
    MACHINERY("Machinery", 25.96),
    RETAIL("Retail", 27.88),
    AUTO_COMPONENTS("Auto Components", 20.80),
    UTILITIES("Utilities", 24.11),
    AIRLINES("Airlines", 31.50),
    ENERGY("Energy", 21.14),
    INDUSTRIAL_CONGLOMERATES("Industrial Conglomerates", 25.14),
    TECHNOLOGY("Technology", 40.96),
    METALS_AND_MINING("Metals & Mining", 33.24),
    BEVERAGES("Beverages", 25.60),
    TEXTILES_APPAREL_AND_LUXURY_GOODS("Textiles, Apparel & Luxury Goods", 18.75),
    DISTRIBUTORS("Distributors", 106.54),
    CONSTRUCTION("Construction", 44.27),
    TRADING_COMPANIES_AND_DISTRIBUTORS("Trading Companies & Distributors", 23.44),
    FINANCIAL_SERVICES("Financial Services", 24.44),
    LOGISTICS_AND_TRANSPORTATION("Logistics & Transportation", 48.77),
    SEMICONDUCTORS("Semiconductors", 60.02),
    BANKING("Banking", 11.99),
    COMMUNICATIONS("Communications", 31.24),
    MEDIA("Media", 29.62),
    TELECOMMUNICATION("Telecommunication", 10.69),
    CONSUMER_PRODUCTS("Consumer Products", 15.00),
    AUTOMOBILES("Automobiles", 133.84),
    CHEMICALS("Chemicals", 25.84),
    FOOD_PRODUCTS("Food Products", 28.09),
    PROFESSIONAL_SERVICES("Professional Services", 19.10),
    HOTELS_RESTAURANTS_AND_LEISURE("Hotels, Restaurants & Leisure", 37.01),
    TOBACCO("Tobacco", 18.95),
    LIFE_SCIENCES_TOOLS_AND_SERVICES("Life Sciences Tools & Services", 28.89),
    HEALTH_CARE("Health Care", 24.58),
    ELECTRICAL_EQUIPMENT("Electrical Equipment", 32.60),
    REAL_ESTATE("Real Estate", 57.63),
    INSURANCE("Insurance", 14.11),
    PACKAGING("Packaging", 15.95),
    COMMERCIAL_SERVICES_AND_SUPPLIES("Commercial Services & Supplies", 29.57),
    PHARMACEUTICALS("Pharmaceuticals", 22.15);

    /**
     * The display name of the industry sector as it appears in the
     * {@link Fortune500} enum's industry field. Must match exactly
     * (case-insensitive) for {@link #getAverageForIndustry(String)}
     * to return a valid result.
     */
    private final String industryName;
    /**
     * The pre-calculated average trailing P/E ratio for this industry sector.
     * Used by {@code CalculationMethods.sectorPEScore()} to compute the
     * ratio of a stock's P/E relative to its industry average, producing
     * a score between 1 and 10.
     */
    private final double averagePE;

    /**
     * Constructs a SectorPE enum constant with the given industry name
     * and pre-calculated average P/E ratio.
     *
     * @param industryName the industry display name matching the corresponding
     *                     industry string in {@link Fortune500}
     *                     (e.g. "Semiconductors", "Banking").
     * @param averagePE    the pre-calculated average trailing P/E ratio
     *                     for this industry sector.
     */
    SectorPE(String industryName, double averagePE) {
        this.industryName = industryName;
        this.averagePE = averagePE;
    }

    /**
     * Looks up and returns the average P/E ratio for the given industry name
     * by iterating all {@code SectorPE} constants and performing a
     * case-insensitive string comparison against each constant's
     * {@code industryName} field.
     *
     * <p>This method is called by {@code CalculationMethods.sectorPEScore()}
     * with the industry string retrieved from {@link Fortune500#getIndustry()}.</p>
     *
     * @param industry the industry classification string to look up,
     *                 sourced from {@link Fortune500#getIndustry()}.
     *                 Comparison is case-insensitive.
     * @return the average P/E ratio for the matching sector as a double,
     *         or {@code 0.0} if no matching sector is found.
     */
    public static double getAverageForIndustry(String industry) {
        for (SectorPE sector : values()) {
            if (sector.industryName.equalsIgnoreCase(industry)) {
                return sector.averagePE;
            }
        }
        return 0;
    }
}
