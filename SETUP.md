# Parallax — New Contributor Setup Guide

**Repo:** `https://github.com/Legitimateleo/stock-analysis-app`  
**Branch:** `feature/cloud-backend`  
**Last updated:** July 2026

---

## What is Parallax

Parallax is a JavaFX desktop application that retrieves real-time financial
data from Finnhub and Polygon.io and computes an intrinsic valuation score
for any publicly traded Fortune 500 company.

For this phase we are building a cloud deployment consisting of:

```
Spring Boot REST API   ← Java backend exposing MarketDataService
Streamlit Dashboard    ← Python frontend consuming the REST API
Docker Compose         ← wires both containers together
Cloud                  ← deployed to AWS, Azure, and GCP
```

The JavaFX desktop app is not being modified.

---

## Prerequisites

Confirm you have the following installed before cloning:

| Tool | Version | Download |
|---|---|---|
| JDK | 21 or later | https://adoptium.net |
| Maven | 3.x | https://maven.apache.org |
| Python | 3.10 or later | https://python.org |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
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

## Step 3 — Configure keys for the desktop app (optional)

If you want to run the original JavaFX desktop app:

Create `src/main/resources/config.properties`:

```properties
FINNHUB_API_KEY=your_finnhub_key_here
POLYGON_API_KEY=your_polygon_key_here
```

This file is in `.gitignore` and must never be committed.

For the cloud deployment the keys are passed as environment variables —
not through this file.

---

## Step 4 — Install Python dependencies

```bash
pip install streamlit requests
```

Verify Streamlit works:

```bash
streamlit hello
```

A browser should open with the Streamlit demo page.

---

## Step 5 — Understand the existing codebase

Read these files before writing any code:

```
src/main/java/team2/parallax/
├── service/
│   ├── MarketDataProvider.java   ← interface — facade contract
│   └── MarketDataService.java    ← Facade — THIS is what we are exposing
├── data/
│   ├── Fortune500.java           ← enum — 397 tickers
│   └── SectorPE.java             ← enum — sector P/E ratios
└── model/
    ├── StockSnapshot.java        ← DTO — all stock metrics
    └── RecommendationTrends.java ← DTO — monthly analyst counts
```

The five public methods on MarketDataService become the five REST endpoints:

```java
search(String input)
getSnapshot(Fortune500 stock)
getTrends(Fortune500 stock)
getValuation(Fortune500 stock, StockSnapshot snapshot)
getByIndustry(Fortune500 stock)
```

---

## Step 6 — Understand the target folder structure

```
stock-analysis-app/
├── backend/          ← Spring Boot REST API (your work if on Track A)
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/         ← Streamlit dashboard (your work if on Track B)
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml
├── src/              ← existing JavaFX source (do not modify)
├── SETUP.md
└── Parallax_Cloud_Plan_of_Attack.md
```

---

## Step 7 — Read the Plan of Attack

Open `Parallax_Cloud_Plan_of_Attack.md` and identify your track before
writing any code. Confirm D4 and D5 with Leonardo before starting Track B.

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
✅ Do not modify the JavaFX desktop entry point FinnhubMain.java
✅ API keys always come from environment variables in containers
✅ config.properties is for the desktop app only
✅ New backend code goes in backend/ folder
✅ New frontend code goes in frontend/ folder
✅ Run mvn clean install before opening a PR on backend changes
✅ Streamlit must call the API by service name not localhost in Docker
```

---

## Questions

Contact Leonardo Avila before making changes to:

```
src/main/java/team2/parallax/service/MarketDataService.java
src/main/java/team2/parallax/api/FinnhubClient.java
src/main/java/team2/parallax/data/Fortune500.java
pom.xml
docker-compose.yml
```