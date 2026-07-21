# Parallax Cloud Deployment — Plan of Attack
### Team 2 · CS446 Final Project · Cloud Extension
**Status:** Active  
**Branch:** `feature/cloud-backend`  
**Authors:** Leo · Shawdy 

---

## 1. What changed and why

Parallax was built as a JavaFX desktop application consuming two external
financial APIs directly from the client. For the cloud deployment phase,
the backend logic must be **extracted and exposed as a standalone REST API**
so it can be containerized and deployed to three cloud platforms.

The JavaFX desktop UI remains untouched. The goal is to lift the existing
Service and API layers out of the desktop application and wrap them in a
lightweight web framework — exposing the same five operations the
`MarketDataService` Facade already provides, now as HTTP endpoints.

---

## 2. Architecture Target

```
Current:
JavaFX UI → ParallaxController → MarketDataService → Finnhub / Polygon

After refactor:
JavaFX UI → ParallaxController → MarketDataService → Finnhub / Polygon
                                        ↑
                              ALSO exposed as:
                         REST API (containerized)
                                   ↓
               AWS  |  Azure  |  Google Cloud
```

**Key rule:** The desktop application is not modified. The REST API is a
parallel deployment of the existing backend — not a replacement.

---

## 3. Scope

### In scope — code transformation only

- Extract the Service Layer and API Layer into a standalone runnable module
- Wrap `MarketDataService` methods as HTTP endpoints
- Externalize API key configuration so containers receive keys as
  environment variables — no hardcoded credentials anywhere
- Produce a single deployable artifact (JAR) the container runs
- Confirm all five facade operations are reachable over HTTP locally
  before any cloud deployment begins

### Out of scope — handled separately

- Cloud deployment steps (AWS, Azure, GCP)
- Infrastructure as code
- Presentation slides
- JavaFX UI changes of any kind
- Authentication on the API endpoints

---

## 4. Endpoints to expose

These map directly to the five public methods on `MarketDataService`:

| HTTP Endpoint | Maps to | Description |
|---|---|---|
| `GET /api/health` | — | Liveness check |
| `GET /api/search?input=` | `search()` | Validate and resolve ticker |
| `GET /api/snapshot?ticker=` | `getSnapshot()` | Full stock metrics |
| `GET /api/trends?ticker=` | `getTrends()` | Analyst recommendations |
| `GET /api/valuation?ticker=` | `getValuation()` | Score + signal |
| `GET /api/related?ticker=` | `getByIndustry()` | Industry peers |

---

## 5. Work breakdown

### Track A — Web Framework Integration
**Owner:** TBD  
**Depends on:** Nothing — starts immediately

High level tasks:
- Introduce a web framework dependency to the project build
- Create a new package for the REST layer separate from existing packages
- Wire `MarketDataService` as a managed component the REST layer can use
- Implement the six endpoints listed above
- Verify all endpoints return JSON locally before handoff

**Stopping point:** Do not proceed to containerization until all six
endpoints are confirmed working locally with live API responses.

---

### Track B — Configuration and Secrets
**Owner:** TBD  
**Depends on:** Nothing — starts immediately, parallel to Track A

High level tasks:
- Remove any dependency on `config.properties` for the REST module
- Ensure API keys are read exclusively from environment variables at runtime
- Confirm the existing desktop app is unaffected by this change
- Document the required environment variable names for the deployment team

**Stopping point:** Do not merge until confirmed that running the REST
module without environment variables set produces a clear error — not a
silent failure or a crash with a confusing stack trace.

---

### Track C — Containerization
**Owner:** TBD  
**Depends on:** Track A complete · Track B complete

High level tasks:
- Produce a build file that compiles the project into a single runnable JAR
- Write a container definition that packages the JAR and its runtime
- Confirm the container accepts API keys as environment variables at startup
- Confirm all six endpoints respond correctly when called against the
  running container on localhost
- Produce the final container image that will be pushed to each cloud registry

**Stopping point:** Do not hand off to cloud deployment until a clean
container build produces correct JSON responses from all six endpoints
on a machine that is not the author's.

---

## 6. Decisions to confirm before Track A begins

- **D1. Web framework choice.** Which framework wraps the REST layer?
  Needs team agreement before Track A writes any code.
- **D2. New module or same module?** Does the REST layer live inside the
  existing Parallax project or in a separate repo/module? This affects
  how the build is structured and whether the desktop app pom is touched.
- **D3. Polygon endpoints included?** The `StockChartPanel` calls
  `PolygonClient` directly and is self-contained. Confirm whether Polygon
  chart data needs to be exposed via the REST API or left desktop-only.

---

## 7. Definition of done — code transformation phase

- A container runs locally and all six endpoints return correct JSON
- API keys are passed as environment variables only — not in any committed file
- The existing JavaFX desktop application builds and runs without modification
- The container image is ready to be pushed to a cloud registry
- A teammate who was not involved in the refactor can run the container
  locally using only the setup guide and confirm it works

---

## 8. Risks

- **D2 decision** — if the REST layer shares the same module as the desktop
  app, JavaFX dependencies may conflict with the web framework at build time.
  Resolve before Track A writes any wiring code.
- **Track A is the critical path.** Tracks B and C cannot fully close
  until the endpoint surface is stable.
- **config.properties** is currently required at runtime by the desktop app.
  Track B must not break the desktop startup flow while removing that
  dependency for the REST module.
- **PolygonClient** (D3) — if chart data endpoints are added later, the
  self-contained nature of `StockChartPanel` means Polygon logic will need
  to be partially duplicated or refactored. Flag this early.