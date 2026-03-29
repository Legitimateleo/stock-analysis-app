package team2.parallax.util;

import com.google.gson.JsonObject;
import team2.parallax.api.FinnhubClient;
import team2.parallax.data.Fortune500;
import team2.parallax.service.MarketDataService;

import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;

public class SectorPECalculator {

    public static void main(String[] args) throws Exception {

        // ── Load API key ─────────────────────────────────────────────
        Properties config = new Properties();
        try (InputStream input = SectorPECalculator.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        }
        String apiKey = config.getProperty("FINNHUB_API_KEY");
        FinnhubClient client = new FinnhubClient(apiKey);
        MarketDataService marketData = new MarketDataService(client);

        // ── Calculate sector averages ─────────────────────────────────
        Map<String, List<Double>> sectorPEs = new HashMap<>();

        for (Fortune500 stock : Fortune500.values()) {
            System.out.println("Processing: " + stock.name());

            JsonObject metrics = marketData.getFinancialMetrics(stock.name());
            if (metrics == null) continue;

            JsonObject m = metrics.getAsJsonObject("metric");
            if (m == null) continue;

            double pe = 0;
            if (m.has("peBasicExclExtraTTM") && !m.get("peBasicExclExtraTTM").isJsonNull()) {
                pe = m.get("peBasicExclExtraTTM").getAsDouble();
            }
            if (pe <= 0) continue;

            String industry = stock.getIndustry();
            sectorPEs.computeIfAbsent(industry, k -> new ArrayList<>()).add(pe);
        }

        // ── Write SectorPE enum to file ───────────────────────────────
        FileWriter writer = null;
        try {
            writer = new FileWriter("sectorpe_enum.txt");
            writer.write("package team2.parallax.data;\n\n");
            writer.write("public enum SectorPE {\n\n");

            List<Map.Entry<String, List<Double>>> entries =
                    new ArrayList<>(sectorPEs.entrySet());

            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, List<Double>> entry = entries.get(i);
                double avg = entry.getValue().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0);

                // convert industry name to enum constant format
                String enumName = entry.getKey()
                        .toUpperCase()
                        .replace(" ", "_")
                        .replace("&", "AND")
                        .replace(",", "")
                        .replace("-", "_")
                        .replace("/", "_");

                String comma = (i == entries.size() - 1) ? ";" : ",";
                String entry_str = String.format("    %s(\"%s\", %.2f)%s\n",
                        enumName, entry.getKey(), avg, comma);

                writer.write(entry_str);
                System.out.printf("  ✓ %-40s → %.2f%n", entry.getKey(), avg);
            }

            writer.write("""

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
                """);

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        } finally {
            if (writer != null) writer.close();
            System.out.println("\n✓ Done! Output written to sectorpe_enum.txt");
        }
    }
}