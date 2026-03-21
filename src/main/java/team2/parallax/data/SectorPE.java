package team2.parallax.data;

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
    TECHNOLOGY("Technology", 29.96),
    METALS_AND_MINING("Metals & Mining", 33.24),
    BEVERAGES("Beverages", 25.60),
    TEXTILES_APPAREL_AND_LUXURY_GOODS("Textiles, Apparel & Luxury Goods", 18.75),
    DISTRIBUTORS("Distributors", 106.54),
    CONSTRUCTION("Construction", 44.27),
    TRADING_COMPANIES_AND_DISTRIBUTORS("Trading Companies & Distributors", 23.44),
    FINANCIAL_SERVICES("Financial Services", 24.44),
    LOGISTICS_AND_TRANSPORTATION("Logistics & Transportation", 48.77),
    SEMICONDUCTORS("Semiconductors", 43.02),
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

    private final String industryName;
    private final double averagePE;

    SectorPE(String industryName, double averagePE) {
        this.industryName = industryName;
        this.averagePE = averagePE;
    }

    public String getIndustryName() { return industryName; }
    public double getAveragePE()    { return averagePE; }

    public static double getAverageForIndustry(String industry) {
        for (SectorPE sector : values()) {
            if (sector.industryName.equalsIgnoreCase(industry)) {
                return sector.averagePE;
            }
        }
        return 0;
    }
}
