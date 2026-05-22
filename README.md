<div align="center">

<img src="docs/parallax_logo.png" alt="Parallax Logo" width="120"/>

# Parallax — Market Intelligence

**A JavaFX desktop application for Fortune 500 stock valuation and analysis.**

[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=java)](https://jdk.java.net/25/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-blue?style=flat-square)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.x-red?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![Finnhub](https://img.shields.io/badge/API-Finnhub-green?style=flat-square)](https://finnhub.io/)
[![Polygon](https://img.shields.io/badge/API-Polygon.io-purple?style=flat-square)](https://polygon.io/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)](LICENSE)

[📹 Watch the Demo on LinkedIn](https://www.linkedin.com/in/leonardo-solorzano) · [⬇️ Download for macOS](#download) · [🚀 Build from Source](#build-from-source)

</div>

---

## Overview

Parallax retrieves real-time financial data from the Finnhub and Polygon.io APIs to compute a composite intrinsic valuation score for any publicly traded Fortune 500 company. It presents this analysis through three core components:

- **Historical price chart** — interactive line chart across 7 timeframes (1D to 2Y) powered by Polygon.io
- **Analyst sentiment chart** — stacked bar chart of Buy/Hold/Sell consensus over 4 months from Finnhub
- **Valuation score (1–10)** — composite score derived from three independent scoring methods with a STRONG BUY → STRONG SELL signal

---

## Features

| Feature | Description |
|---|---|
| 🔍 Stock Search | Search by ticker symbol or company name across 397 Fortune 500 equities |
| 📈 Price Chart | Interactive canvas chart with 7 timeframes, hover tooltips, and gradient fills |
| 🧮 Valuation Engine | Three-method scoring: Forward P/E · 52-Week Range · Sector P/E comparison |
| 📊 Analyst Trends | Stacked bar chart of analyst consensus from Finnhub recommendation data |
| 🏢 Related Stocks | One-click industry peer discovery — zero API calls via Fortune500 enum |
| 💾 Smart Caching | In-memory chart cache eliminates redundant Polygon.io API calls |

---

## Tech Stack

```
Language        Java 25
UI Framework    JavaFX 23 (Canvas rendering, StackedBarChart, custom controls)
Build Tool      Maven 3.x
JSON Parsing    Google Gson 2.10.1
APIs            Finnhub.io (real-time quotes, metrics, recommendations)
                Polygon.io (historical OHLCV aggregates)
Architecture    Layered (UI → Service → Data/Model → API)
Patterns        Facade · MVC · DAO · Interface Contracts · Enum as shared kernel
```

---

## Architecture

```
team2.parallax/
├── ui/          MainWindow (View) · ParallaxController (Controller)
│                StockChartPanel · RecommendationTrendsChart · ViewCallBack
├── service/     MarketDataService (Facade) · CalculationMethods · ValidationScore
├── model/       StockSnapshot · RecommendationTrends
├── data/        Fortune500 «enum» · SectorPE «enum»
└── api/         FinnhubClient · PolygonClient · DataAccessClient · ChartDataClient
```

The Facade pattern is the architectural centrepiece — `MarketDataService` exposes five clean methods to the UI while hiding all JSON parsing, rate limiting, and multi-endpoint orchestration internally. No `JsonObject` ever crosses into the UI layer.

---

## Prerequisites

- **JDK 25** — [Download here](https://jdk.java.net/25/)
- **Apache Maven 3.x** — [Download here](https://maven.apache.org/)
- **Finnhub API key** — Free tier at [finnhub.io/register](https://finnhub.io/register)
- **Polygon.io API key** — Free tier at [polygon.io/dashboard/signup](https://polygon.io/dashboard/signup)

---

## Build from Source

### 1. Clone the repository

```bash
git clone https://github.com/Legitimateleo/stock-analysis-app.git
cd stock-analysis-app
```

### 2. Create your config file

Create the file `src/main/resources/config.properties`:

```bash
touch src/main/resources/config.properties
```

Open it and add your API keys:

```properties
FINNHUB_API_KEY=your_finnhub_key_here
POLYGON_API_KEY=your_polygon_key_here
```

> ⚠️ `config.properties` is listed in `.gitignore` and will never be committed. Never share your API keys publicly.

### 3. Mark the resources directory (IntelliJ)

```
Right-click src/main/resources
→ Mark Directory As → Resources Root
```

### 4. Install dependencies

```bash
mvn clean install
```

### 5. Run the application

```bash
mvn javafx:run
```

Or run `team2.parallax.FinnhubMain` directly from your IDE.

---

## Download

### macOS

> Requires macOS 12 or later. No Java installation needed.

1. Download `Parallax_MacOs.zip` from the [Releases](https://github.com/Legitimateleo/stock-analysis-app/releases) page
2. Extract and double-click `Parallax_MacOs.app`
3. If blocked by Gatekeeper:
   - Go to **System Settings → Privacy & Security**
   - Click **Open Anyway**
   - Or right-click the app → **Open** → **Open**

### Windows

> Requires Windows 10 or later. No Java installation needed — runtime is bundled.

1. Download `Parallax_Windows.zip` from the [Releases](https://github.com/Legitimateleo/stock-analysis-app/releases) page
2. Extract the entire zip — **do not move** `Parallax.exe` out of the folder
3. Double-click `Parallax.exe`

---

## API Rate Limits

| API | Free Tier Limit | Parallax Usage |
|---|---|---|
| Finnhub | 60 req/min | 3–4 calls per search · 1 for trends |
| Polygon.io | 5 req/min | 1 call per chart timeframe (cached after first load) |

Parallax implements a 35ms delay between Finnhub requests and a 500ms delay for Polygon to stay within free tier limits automatically.

---

## Valuation Scoring

The valuation engine averages three independent scores (each 1–10):

| Method | Description |
|---|---|
| Forward P/E Score | Compares trailing P/E against historical market average brackets |
| 52-Week Range Score | Deviation of current price from the 52-week high/low midpoint |
| Sector P/E Score | Stock P/E vs industry average from pre-calculated SectorPE enum (0 API calls) |

**Signals:** ≥ 7.0 → `STRONG BUY` · ≥ 5.1 → `BUY` · 5.0 → `HOLD` · ≥ 3.0 → `SELL` · < 3.0 → `STRONG SELL`

---

## Project Structure

```
stock-analysis-app/
├── src/
│   └── main/
│       ├── java/team2/parallax/
│       │   ├── api/                 FinnhubClient · PolygonClient · interfaces
│       │   ├── data/                Fortune500 · SectorPE enums
│       │   ├── model/               StockSnapshot · RecommendationTrends
│       │   ├── service/             MarketDataService · CalculationMethods · ValidationScore
│       │   ├── ui/                  MainWindow · ParallaxController · StockChartPanel
│       │   └── FinnhubMain.java     Application entry point
│       └── resources/
│           └── config.properties    ← you create this (see setup above)
├── docs/
│   └── Parallax_Icon.png
├── .gitignore
├── pom.xml
└── README.md
```

---

## Team

Built as a semester-long software engineering project simulating a professional development environment under IEEE SDS standards.

| Name | Role |
|---|---|
| **Leonardo Solorzano** | Backend architecture · API integration · Service layer · MVC design |
| **Robert Huntington** | Recommendation trends chart · Search algorithm · Related stock navigation |
| **Patrick Valdivia** | StockChartPanel · Polygon.io integration · UI aesthetics |
| **Kyle Bertrand** | Documentation & Testing|

---

## Connect

**Leonardo Solorzano** — [LinkedIn](https://www.linkedin.com/in/leonardo-solorzano)

📹 [Watch the full demo on LinkedIn](https://www.linkedin.com/in/leonardo-solorzano) — includes a walkthrough of the valuation engine, chart interaction, and analyst trends visualization.

---

## License

> API keys are required to run this application. Obtain free keys at [finnhub.io](https://finnhub.io/register) and [polygon.io](https://polygon.io/dashboard/signup).
