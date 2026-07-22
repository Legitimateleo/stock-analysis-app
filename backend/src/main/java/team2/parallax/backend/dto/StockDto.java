package team2.parallax.backend.dto;

import team2.parallax.data.Fortune500;

/**
 * Flat JSON-friendly view of a {@link Fortune500} enum constant.
 * The enum itself isn't returned directly from controllers because
 * Jackson would only serialize the constant name, dropping companyName
 * and industry.
 */
public record StockDto(String ticker, String companyName, String industry) {
    public static StockDto from(Fortune500 stock) {
        return new StockDto(stock.name(), stock.getCompanyName(), stock.getIndustry());
    }
}
