package team2.parallax.data;

public enum Fortune500 {
    AAPL("Apple Inc", "Technology"),
    ;


    private final String companyName;
    private final String industry;

    Fortune500(String companyName, String industry) {
        this.companyName = companyName;
        this.industry = industry;
    }

    public String getCompanyName() {
        return this.companyName;
    }
    public String getIndustry() {
        return this.industry;
    }

}
