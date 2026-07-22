"""
theme.py — shared constants and small rendering helpers so every page
looks consistent without copy-pasting styling logic five times.
"""

import streamlit as st

# Matches the dark blue used in the desktop app's class diagrams / accent color
ACCENT = "#1F3864"
BACKGROUND = "#0E1117"  # Streamlit's own default dark background
CARD_BG = "#1A1C24"

SIGNAL_COLORS = {
    "STRONG BUY": "#1DB954",
    "BUY": "#63D471",
    "HOLD": "#E8C547",
    "SELL": "#E67E22",
    "STRONG SELL": "#E74C3C",
}

RANGE_OPTIONS = ["1D", "1W", "1M", "3M", "1Y", "5Y"]


def signal_badge(signal: str) -> str:
    """Returns an HTML snippet rendering the signal as a colored pill."""
    color = SIGNAL_COLORS.get(signal, "#888888")
    return (
        f'<span style="background-color:{color}; color:#0E1117; '
        f'padding:4px 14px; border-radius:999px; font-weight:700; '
        f'font-size:0.95rem;">{signal}</span>'
    )


def require_stock():
    """
    Every page except Home depends on a stock already being selected.
    Call this at the top of each page — it stops rendering and shows a
    friendly prompt if nothing's been searched yet, instead of crashing
    on a missing session_state key.
    """
    if "ticker" not in st.session_state or not st.session_state["ticker"]:
        st.info("Search for a stock on the **Home** page first.")
        st.stop()
    return st.session_state["ticker"], st.session_state.get("stock", {})


def format_market_cap(value: float) -> str:
    """Finnhub returns market cap in millions of dollars."""
    if value >= 1_000_000:
        return f"${value / 1_000_000:.2f}T"
    if value >= 1_000:
        return f"${value / 1_000:.2f}B"
    return f"${value:.1f}M"
