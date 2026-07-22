"""
api_client.py — the only module in this app that talks HTTP to the
Spring Boot backend. Every page imports from here rather than calling
`requests` directly, so the base URL, timeout, and error handling stay
in one place.

D5 resolution: the backend's address is read from the API_BASE_URL
environment variable.
  - Local dev (backend running via `mvn spring-boot:run` on your host):
        API_BASE_URL=http://localhost:8080   (the default below)
  - Docker Compose (Track C): Streamlit must reach the backend by its
    Compose *service name*, not localhost — e.g. if the backend service
    in docker-compose.yml is named "backend":
        API_BASE_URL=http://backend:8080
"""

import os
import requests

API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8080")
TIMEOUT_SECONDS = 10


class ApiError(Exception):
    """Raised for any non-2xx response or connection failure, with a
    message that's safe to show directly in the Streamlit UI."""
    pass


def _get(path: str, params: dict | None = None) -> dict | list:
    try:
        resp = requests.get(f"{API_BASE_URL}{path}", params=params, timeout=TIMEOUT_SECONDS)
    except requests.exceptions.ConnectionError:
        raise ApiError(
            f"Can't reach the Parallax API at {API_BASE_URL}. "
            "Is the Spring Boot backend running?"
        )
    except requests.exceptions.Timeout:
        raise ApiError(f"Request to {API_BASE_URL}{path} timed out.")

    if resp.status_code == 404:
        try:
            detail = resp.json().get("error", "Not found")
        except ValueError:
            detail = "Not found"
        raise ApiError(detail)

    if resp.status_code == 400:
        try:
            detail = resp.json().get("error", "Bad request")
        except ValueError:
            detail = "Bad request"
        raise ApiError(detail)

    if not resp.ok:
        raise ApiError(f"Backend returned HTTP {resp.status_code} for {path}")

    return resp.json()


def health() -> dict:
    return _get("/api/health")


def search(query: str) -> dict:
    """Returns {'ticker', 'companyName', 'industry'}."""
    return _get("/api/search", {"input": query})


def get_snapshot(ticker: str) -> dict:
    return _get("/api/snapshot", {"ticker": ticker})


def get_trends(ticker: str) -> list:
    return _get("/api/trends", {"ticker": ticker})


def get_valuation(ticker: str) -> dict:
    """Returns {'score', 'signal'}."""
    return _get("/api/valuation", {"ticker": ticker})


def get_related(ticker: str) -> list:
    return _get("/api/related", {"ticker": ticker})


def get_chart(ticker: str, range_code: str) -> list:
    """range_code: one of 1D, 1W, 1M, 3M, 1Y, 5Y."""
    return _get("/api/chart", {"ticker": ticker, "range": range_code})
