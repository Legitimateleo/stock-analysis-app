"""
pages/4_Price_Chart.py — use case 5: user changes the historical price
graph's range. Sourced from GET /api/chart, which is backed by Polygon
via ChartDataService (independent of MarketDataService — see D4 in the
Plan of Attack).
"""

import streamlit as st
import plotly.graph_objects as go
from datetime import datetime
from api_client import get_chart, ApiError
from theme import require_stock, RANGE_OPTIONS

st.set_page_config(page_title="Price Chart — Parallax", page_icon="📉", layout="wide")

ticker, stock = require_stock()

st.title(f"📉 Price Chart — {ticker}")
if stock.get("companyName"):
    st.caption(stock["companyName"])

selected_range = st.radio(
    "Range",
    RANGE_OPTIONS,
    horizontal=True,
    index=RANGE_OPTIONS.index(st.session_state.get("chart_range", "1M")),
    label_visibility="collapsed",
)

# Re-fetch whenever the ticker or the selected range changes — this is
# the "change the graph's scope" interaction itself.
needs_fetch = (
    "chart_data" not in st.session_state
    or st.session_state.get("chart_range") != selected_range
    or st.session_state.get("chart_ticker") != ticker
)

if needs_fetch:
    with st.spinner(f"Loading {selected_range} chart..."):
        try:
            bars = get_chart(ticker, selected_range)
        except ApiError as e:
            st.error(str(e))
            st.stop()
    st.session_state["chart_data"] = bars
    st.session_state["chart_range"] = selected_range
    st.session_state["chart_ticker"] = ticker

bars = st.session_state["chart_data"]

if not bars:
    st.warning(
        "No chart data returned. This can mean POLYGON_API_KEY isn't set "
        "on the backend, or there's no data for this range."
    )
else:
    dates = [datetime.fromtimestamp(b["timestamp"] / 1000) for b in bars]

    fig = go.Figure(data=[go.Candlestick(
        x=dates,
        open=[b["open"] for b in bars],
        high=[b["high"] for b in bars],
        low=[b["low"] for b in bars],
        close=[b["close"] for b in bars],
        increasing_line_color="#1DB954",
        decreasing_line_color="#E74C3C",
    )])

    fig.update_layout(
        template="plotly_dark",
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        xaxis_rangeslider_visible=False,
        height=500,
        yaxis_title="Price ($)",
    )

    st.plotly_chart(fig, use_container_width=True)

    with st.expander("Volume"):
        vol_fig = go.Figure(data=[go.Bar(
            x=dates,
            y=[b["volume"] for b in bars],
            marker_color="#1F3864",
        )])
        vol_fig.update_layout(
            template="plotly_dark",
            paper_bgcolor="rgba(0,0,0,0)",
            plot_bgcolor="rgba(0,0,0,0)",
            height=200,
        )
        st.plotly_chart(vol_fig, use_container_width=True)
