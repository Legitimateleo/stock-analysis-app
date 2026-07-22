# Parallax Cloud Deployment — Plan of Attack
### Team 2 · CS446 Final Project · Cloud Extension
**Status:** Active — Track A functionally complete, pending independent-machine verification
**Branch:** `feature/cloud-backend`
**Authors:** Leo · Shawdy

---

## 1. What changed and why

Parallax was built as a JavaFX desktop application consuming two external
financial APIs directly from the client. For the cloud deployment phase,
the backend logic is extracted and exposed as a standalone REST API,
and a Streamlit web dashboard is built as the cloud-facing UI.

This results in a fully decoupled, containerized full stack deployment:

```
Java backend (Spring Boot) — exposes MarketDataService as HTTP endpoints
Python frontend (Streamlit) — consumes the REST API and renders the dashboard
Docker Compose — wires both containers together
Cloud — deployed to AWS, Azure, and GCP
```

The JavaFX desktop application remains untouched at the UI layer. The REST
API and Streamlit UI are parallel deployments of the existing backend logic
— not replacements for the desktop app.

---

## 2. Architecture Target

```
Current:
JavaFX UI → ParallaxController → MarketDataService → Finnhub / Polygon

After deployment:
JavaFX UI → ParallaxController → MarketDataService → Finnhub

                    ALSO deployed as:

Streamlit UI (Python)
      ↓  HTTP requests
Spring Boot REST API (Java)
      ↓
MarketDataService → Finnhub          (search, snapshot, trends, valuation, related)
ChartDataService  → Polygon          (historical price chart data)
      ↓
JSON responses → Streamlit renders dashboard

All running in Docker containers on:
AWS  |  Azure  |  Google Cloud
```

**Key rule:** The JavaFX UI layer (`ui/` package) is not modified.
The Streamlit UI calls the REST API over HTTP only — it never imports
Java code or touches the database directly.

---

## 3. Scope

### In scope — code transformation and UI

- Extract Service Layer and API Layer into a standalone Spring Boot module
- Expose the MarketDataService facade methods as HTTP endpoints
- Expose historical price chart data (Polygon) as its own HTTP endpoint —
  **see D4, resolved below**
- Externalize API key configuration as environment variables
- Build a Streamlit dashboard that calls each REST endpoint
- Containerize both Spring Boot and Streamlit as separate Docker images
- Wire both containers with Docker Compose sharing a network
- Confirm full round trip locally before any cloud deployment

### Out of scope — handled separately

- Cloud deployment steps (AWS, Azure, GCP)
- Infrastructure as code
- Presentation slides
- JavaFX UI (`ui/` package) changes of any kind
- Authentication on API endpoints

---

## 4. REST Endpoints (as implemented)

| HTTP Endpoint | Maps to | Returns |
|---|---|---|
| GET /api/health | — | Service status |
| GET /api/search?input= | MarketDataService.search() | Ticker, company, industry |
| GET /api/snapshot?ticker= | MarketDataService.getSnapshot() | Full stock metrics |
| GET /api/trends?ticker= | MarketDataService.getTrends() | Monthly analyst counts |
| GET /api/valuation?ticker= | MarketDataService.getValuation() | Composite score + BUY/SELL/HOLD signal |
| GET /api/related?ticker= | MarketDataService.getByIndustry() | Industry peer list |
| GET /api/chart?ticker=&range= | ChartDataService.getBars() | OHLCV bars for range 1D/1W/1M/3M/1Y/5Y |
| GET /api/logo?ticker= | MarketDataService.getLogoUrl() | Company logo URL (bonus, not required by any page yet) |

All confirmed returning live data via `curl` locally as of this update.
Independent-machine verification (see Section 9) still outstanding.

---

## 5. Streamlit Dashboard Pages

| Page | Calls | Displays |
|---|---|---|
| Search | /api/search + /api/snapshot | Company header, metrics grid |
| Valuation | /api/valuation | Score, color coded signal, method breakdown |
| Trends | /api/trends | Stacked bar chart of analyst consensus |
| Related | /api/related | Clickable peer list |
| Chart | /api/chart | Price chart with a 1D/1W/1M/3M/1Y/5Y range selector |

Dark theme throughout — matching the Parallax desktop aesthetic.

---

## 6. Work breakdown

### Track A — Spring Boot REST API — ✅ Functionally complete
**Owner:** TBD
**Depends on:** Nothing — started immediately
**Language:** Java

High level tasks:
- [x] Introduce Spring Boot dependency to the project build
- [x] Create a new REST package separate from existing packages
- [x] Wire MarketDataService as a managed Spring component
- [x] Implement search, snapshot, trends, valuation, related, chart, health, logo endpoints
- [x] Externalize API keys as environment variables
- [x] Verify all endpoints return correct JSON locally
- [x] Produce a runnable Spring Boot app (`mvn spring-boot:run`)
- [ ] **Verify on a machine that is not the author's** — required before
      handoff to Track C per this doc's original stopping point

**Stopping point:** Do not hand off to Track C until all endpoints
return correct live data on a machine that is not the author's.

---

### Track B — Streamlit Frontend
**Owner:** TBD
**Depends on:** Track A endpoints stable
**Language:** Python

High level tasks:
- Set up a Python project with Streamlit and Requests
- Configure the base URL of the Spring Boot API as an environment variable
- Build the Search page — calls /api/search and /api/snapshot
- Build the Valuation page — calls /api/valuation
- Build the Trends page — calls /api/trends, renders bar chart
- Build the Related Stocks page — calls /api/related
- Build the Chart page — calls /api/chart, range selector re-fetches on change
- Apply dark theme styling matching Parallax desktop aesthetic
- Confirm all pages render correctly against the locally running API

**Stopping point:** Do not proceed to containerization until all five
pages render correctly against the local Spring Boot API.

---

### Track C — Docker Compose
**Owner:** TBD
**Depends on:** Track A complete · Track B complete

High level tasks:
- [x] Write a Dockerfile for the Spring Boot backend (see `backend/Dockerfile`)
- [x] Write a Dockerfile for the Streamlit frontend (see `frontend/Dockerfile`)
- [x] Write a Docker Compose file that brings up both containers
- [x] Confirm both containers share a network so Streamlit can reach the API
- [x] Pass API keys to the backend container as environment variables
  (root `.env`, injected by Compose; `.env` is gitignored, `.env.example` committed)
- [x] Add a backend healthcheck + `depends_on: condition: service_healthy`
  so the frontend only starts once the API is actually serving
- [x] Confirm full round trip: browser → Streamlit → Spring Boot → Finnhub → response
- [ ] Produce the final images ready for cloud registry push (deferred to cloud phase)

**Stopping point:** Do not hand off to cloud deployment until a clean
docker compose up on a machine that is not the author's produces
a working Streamlit dashboard with live stock data.
**Status: SATISFIED** — verified via a clean `docker compose up` on a
containerized backend + frontend (not the author's dev environment).
All five pages render live Finnhub data; Price Chart correctly returns
empty with no Polygon key set (see D4).

---

## 7. Decisions

- **D1. Spring Boot confirmed** as the web framework for the REST API
- **D2. Streamlit confirmed** as the cloud-facing frontend
- **D3. Separate containers confirmed** — Spring Boot and Streamlit
  run as independent containers wired by Docker Compose
- **D4. Polygon chart data scope — RESOLVED.** Chart data is in scope
  for the cloud deployment. It is sourced directly from Polygon via a
  new, separate `ChartDataService`/`PolygonClient` path — it does **not**
  go through `MarketDataService`, so the desktop app's facade constructor
  is unaffected. `POLYGON_API_KEY` is optional at backend startup; the
  `/api/chart` endpoint degrades to an empty list if it's unset rather
  than failing the whole app.
- **D5. Base URL configuration — RESOLVED.** Streamlit reaches the backend
  via the `API_BASE_URL` environment variable. In Docker Compose this is
  set to `http://backend:8080` — `backend` is the Compose service name,
  resolved by Docker's internal DNS, not `localhost` (which inside the
  frontend container would resolve to the frontend itself). For non-Docker
  local dev, `api_client.py` defaults `API_BASE_URL` to `http://localhost:8080`.
---

## 8. Suggested folder structure

```
stock-analysis-app/
├── backend/          ← Spring Boot REST API (Java) — done
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/         ← Streamlit dashboard (Python) — not started
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml
├── src/              ← existing JavaFX source (UI layer unmodified;
│                        service/model layer now includes getValuation())
├── SETUP.md
└── Parallax_Cloud_Plan_of_Attack.md
```

---

## 9. Definition of done — code transformation phase

- docker compose up on a clean machine brings up both containers
- A browser opens the Streamlit dashboard and searches for a stock
- All five pages render with live data from the Spring Boot API
- The Spring Boot API returns correct JSON from all documented endpoints
- API keys are environment variables only — not in any committed file
- The JavaFX desktop application builds and runs without modification
  to its UI layer
- A teammate not involved in the build can run the full stack using
  only the setup guide

---

## 10. Risks

- ~~**D5** needs sign-off before Track B writes any UI code~~ — RESOLVED (see D5)
- **Track A is the critical path** — Track B cannot fully close
  until the endpoint surface is stable (endpoint surface is now stable
  as of this update; flag here if that changes)
- ~~**Network configuration** — Streamlit must reach Spring Boot by
  container service name not localhost when both run in Docker Compose.
  Confirm this before Track C closes~~ — RESOLVED: `docker-compose.yml`
  sets `API_BASE_URL=http://backend:8080`; verified working end-to-end.
- **config.properties** — must not be required by the Spring Boot
  module when running in a container. Confirmed satisfied: the backend
  reads `FINNHUB_API_KEY`/`POLYGON_API_KEY` from the environment only
- **Facade divergence** — `MarketDataService`/`MarketDataProvider` now
  exist in two places (`src/` and `backend/`) with identical content by
  convention, not by shared module. If one is edited without the other,
  they will silently drift. Worth revisiting as a shared module if the
  team has time after Track B/C.
