# Parallax — New Contributor Setup Guide

**Repo:** `https://github.com/Legitimateleo/stock-analysis-app`  
**Branch:** `feature/cloud-backend`  
**Last updated:** May 2026

---

## What is Parallax

Parallax is a JavaFX desktop application that retrieves real-time financial
data from two external APIs — Finnhub and Polygon.io — and computes an
intrinsic valuation score for any publicly traded Fortune 500 company.

For this phase we are extracting the backend into a standalone REST API
so it can be containerized and deployed to cloud platforms. The desktop
app is not being modified.

---

## Prerequisites

Before cloning confirm you have the following installed:

| Tool | Version | Download |
|---|---|---|
| JDK | 21 or later | https://adoptium.net |
| Maven | 3.x | https://maven.apache.org |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |

---

## Step 1 — Clone the repo

```bash
git clone https://github.com/Legitimateleo/stock-analysis-app.git
cd stock-analysis-app
```

Checkout the working branch:

```bash
git checkout feature/cloud-backend
```

---

## Step 2 — Get your API keys

You need two free API keys. Obtain them before running anything.

**Finnhub** — real-time quotes, metrics, recommendations
```
https://finnhub.io/register
Free tier: 60 requests per minute
```

**Polygon.io** — historical price chart data
```
https://polygon.io/dashboard/signup
Free tier: 5 requests per minute
```

---

## Step 3 — Configure your keys

Create the file `src/main/resources/config.properties`:

```bash
touch src/main/resources/config.properties
```

Add your keys:

```properties
FINNHUB_API_KEY=your_finnhub_key_here
POLYGON_API_KEY=your_polygon_key_here
```

> ⚠️ This file is in `.gitignore` and must never be committed.
> Never share your API keys in Slack, PRs, or commit messages.

---

## Step 4 — Mark resources directory (IntelliJ only)

```
Right-click src/main/resources
→ Mark Directory As → Resources Root
```

---

## Step 5 — Build the project

```bash
mvn clean install
```

If the build fails check that your JDK version is 21 or later:

```bash
java -version
```

---

## Step 6 — Run the desktop app (sanity check)

```bash
mvn javafx:run
```

The Parallax window should open. Search for a ticker like `NVDA` to confirm
the API keys are working. If the search returns data you are set up correctly.

---

## Step 7 — Understand the codebase before writing any code

Read these files in order before touching anything:

```
src/main/java/team2/parallax/
├── api/
│   ├── DataAccessClient.java     ← interface — Finnhub contract
│   ├── ChartDataClient.java      ← interface — Polygon contract
│   ├── FinnhubClient.java        ← DAO — all Finnhub HTTP calls
│   └── PolygonClient.java        ← DAO — all Polygon HTTP calls
├── service/
│   ├── MarketDataProvider.java   ← interface — facade contract
│   └── MarketDataService.java    ← Facade — THIS is what we are exposing
├── data/
│   ├── Fortune500.java           ← enum — 397 tickers + company names
│   └── SectorPE.java             ← enum — sector average P/E ratios
└── model/
    ├── StockSnapshot.java        ← DTO — all stock metrics
    └── RecommendationTrends.java ← DTO — monthly analyst counts
```

The five public methods on `MarketDataService` are what we are wrapping
as REST endpoints:

```java
search(String input)                              // validate ticker
getSnapshot(Fortune500 stock)                     // fetch metrics
getTrends(Fortune500 stock)                       // fetch analyst data
getValuation(Fortune500 stock, StockSnapshot s)   // compute score
getByIndustry(Fortune500 stock)                   // find peers
```

---

## Step 8 — Check your branch is up to date before writing code

```bash
git fetch origin
git status
git pull origin feature/cloud-backend
```

---

## Key rules for this codebase

```
✅ The desktop JavaFX app must not be modified
✅ API keys always come from environment variables in the REST module
✅ config.properties is for the desktop app only — never for the container
✅ No JsonObject or JsonArray should ever leave MarketDataService
✅ New code goes in a new package — do not modify existing packages
✅ Run mvn clean install before opening a PR
```

---

## Questions

Reach out to Leo before making any changes to:

```
MarketDataService.java    ← core facade
FinnhubClient.java        ← rate limiting logic
Fortune500.java           ← do not add or remove entries
pom.xml                   ← dependency changes need team sign-off
```