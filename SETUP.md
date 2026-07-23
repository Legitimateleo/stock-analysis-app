# Parallax — New Contributor Setup Guide

**Repo:** `https://github.com/Legitimateleo/stock-analysis-app`
**Branch:** `feature/cloud-backend`
**Last updated:** July 2026

---

## What is Parallax

Parallax is a JavaFX desktop application that retrieves real-time financial
data from Finnhub and Polygon.io and computes an intrinsic valuation score
for any publicly traded Fortune 500 company.

The cloud deployment adds:

```
Spring Boot REST API   ← Java backend exposing MarketDataService + ChartDataService
Streamlit Dashboard    ← Python frontend consuming the REST API
Docker Compose         ← wires both containers together (Track C, not built yet)
Cloud                  ← deployed to AWS, Azure, and GCP (not started yet)
```

The JavaFX desktop UI (`src/main/java/team2/parallax/ui/`) is not modified.

---

## Prerequisites

Confirm you have the following installed before cloning:

| Tool | Version | Download |
|---|---|---|
| JDK | 21 or later | https://adoptium.net |
| Maven | 3.x | https://maven.apache.org |
| Python | 3.10 or later | https://python.org |
| Docker Desktop | Latest (needed once Track C exists) | https://www.docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |

---

## Step 1 — Clone the repo

```bash
git clone https://github.com/Legitimateleo/stock-analysis-app.git
cd stock-analysis-app
git checkout feature/cloud-backend
```

---

## Step 2 — Get your API keys

You need two free API keys before running anything.

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

## Step 3 — Run the Spring Boot backend

The backend does **not** use `config.properties` — it reads keys from
environment variables only.

```bash
cd backend
export FINNHUB_API_KEY=your_finnhub_key_here
export POLYGON_API_KEY=your_polygon_key_here   # optional — chart endpoint
                                                 # returns [] without it,
                                                 # everything else still works
mvn spring-boot:run
```

Leave this running. In a second terminal, confirm it's alive:

```bash
curl "http://localhost:8080/api/health"
curl "http://localhost:8080/api/search?input=nvidia"
```

Both should return JSON. If `search` comes back empty or errors, double
check `FINNHUB_API_KEY` is set correctly in the terminal you ran
`mvn spring-boot:run` from.

Full endpoint list: `/api/health`, `/api/search?input=`,
`/api/snapshot?ticker=`, `/api/trends?ticker=`, `/api/valuation?ticker=`,
`/api/related?ticker=`, `/api/chart?ticker=&range=1D|1W|1M|3M|1Y|5Y`,
`/api/logo?ticker=`.

---

## Step 4 — Run the Streamlit frontend

Open a **third** terminal (keep the backend running in the second one).

```bash
cd frontend
pip install -r requirements.txt
export API_BASE_URL=http://localhost:8080   # points at the backend from Step 3
streamlit run app.py
```

Your browser should open `http://localhost:8501` automatically. Search for
a ticker (e.g. `NVDA`) on the Home page — if the snapshot header and
metrics grid render with real numbers, both the backend and frontend are
correctly wired end-to-end.

Use the sidebar to check the other four pages: Valuation, Trends, Related,
Price Chart.

---

## Step 5 — (Optional) Run the original JavaFX desktop app

If you want to run the desktop app instead of/alongside the cloud stack:

Create `src/main/resources/config.properties` (this file is
`.gitignore`d — never commit it):

```properties
FINNHUB_API_KEY=your_finnhub_key_here
POLYGON_API_KEY=your_polygon_key_here
```

Then from the repo root:

```bash
mvn clean install
mvn javafx:run
```

---

## Step 6 — Understand the codebase

```
src/main/java/team2/parallax/
├── service/
│   ├── MarketDataProvider.java   ← interface — facade contract
│   ├── MarketDataService.java    ← Facade — search, snapshot, trends, valuation, related
│   ├── CalculationMethods.java   ← individual valuation scoring methods
│   └── ValidationScore.java      ← combines scores into final score + signal
├── data/
│   ├── Fortune500.java           ← enum — 397 tickers
│   └── SectorPE.java             ← enum — sector P/E ratios
└── model/
    ├── StockSnapshot.java        ← DTO — all stock metrics
    ├── RecommendationTrends.java ← DTO — monthly analyst counts
    └── ValuationResult.java      ← DTO — score + signal

backend/src/main/java/team2/parallax/backend/
├── controller/
│   ├── MarketDataController.java ← search/snapshot/trends/valuation/related/logo/health
│   └── ChartDataController.java  ← /api/chart (Polygon, separate from MarketDataService)
├── service/
│   ├── ChartDataService.java     ← wraps PolygonClient, parses to ChartPoint DTOs
│   └── ChartRange.java           ← maps 1D/1W/1M/3M/1Y/5Y to Polygon query params
├── dto/                          ← StockDto, ErrorResponse
└── config/AppConfig.java         ← wires beans, reads API keys from env vars

frontend/
├── app.py                        ← Home page: search (use case 1)
├── pages/
│   ├── 1_Valuation.py            ← use case 3
│   ├── 2_Trends.py               ← use case 4
│   ├── 3_Related.py              ← use case 2
│   └── 4_Price_Chart.py          ← use case 5
├── api_client.py                 ← the only file that calls requests.get()
└── theme.py                      ← shared styling + require_stock() guard
```

Note: `MarketDataService`/`MarketDataProvider` currently exist **twice** —
once under `src/` (used by the desktop app) and once under `backend/`
(used by the Spring Boot module). They're kept identical by convention,
not by a shared module. If you edit one, edit the other the same way, or
ping Leonardo.

---

## Step 7 — Read the Plan of Attack

Open `Parallax_Cloud_Plan_of_Attack.md` for the full endpoint table,
work breakdown by track, and the current decision log (D1–D5). Track A
and Track B are both merged as of this update — Track C (Docker Compose)
is next.

---

## Step 8 — Keep your branch up to date

```bash
git fetch origin
git pull origin feature/cloud-backend
```

Run this at the start of every working session.

---

## Key rules

```
✅ Do not modify any file under src/main/java/team2/parallax/ui/
✅ Do not modify the JavaFX desktop entry point
✅ API keys always come from environment variables in the backend/frontend
✅ config.properties is for the desktop app only
✅ New backend code goes in backend/ folder
✅ New frontend code goes in frontend/ folder
✅ Run mvn clean install (desktop) and mvn compile (backend/) before opening a PR
✅ Streamlit must call the API by Docker Compose service name, not localhost,
   once Track C exists — see API_BASE_URL in frontend/.env.example
```

---

## Questions

Contact Leonardo Solorzano before making changes to:

```
src/main/java/team2/parallax/service/MarketDataService.java
backend/src/main/java/team2/parallax/service/MarketDataService.java
src/main/java/team2/parallax/api/FinnhubClient.java
src/main/java/team2/parallax/data/Fortune500.java
pom.xml
backend/pom.xml
docker-compose.yml (once it exists)
```