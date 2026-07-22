"""
app.py — Home page. Covers use case 1 (search) and displays the
snapshot header/metrics grid once a stock is found, matching what
StockSnapshot carries on the desktop app.

Streamlit auto-discovers files in pages/ and builds the sidebar nav
from this file + that folder — no manual routing needed.
"""

import streamlit as st
from api_client import search, get_snapshot, ApiError
from theme import format_market_cap

st.set_page_config(page_title="Parallax", page_icon="📈", layout="wide")

st.title("📈 Parallax")
st.caption("Search any Fortune 500 ticker or company name to get started.")

query = st.text_input(
    "Search",
    placeholder="e.g. NVDA or Nvidia",
    label_visibility="collapsed",
)


def render_snapshot(stock: dict, snapshot: dict):
    header_col1, header_col2 = st.columns([1, 5])
    with header_col1:
        if snapshot.get("logo") and snapshot["logo"] != "N/A":
            st.image(snapshot["logo"], width=64)
    with header_col2:
        st.subheader(f"{stock['companyName']} ({stock['ticker']})")
        st.caption(stock["industry"])

    change = snapshot["change"]
    change_pct = snapshot["changePercent"]
    price_color = "green" if change >= 0 else "red"
    arrow = "▲" if change >= 0 else "▼"
    st.markdown(
        f"### ${snapshot['currentPrice']:.2f}  "
        f":{price_color}[{arrow} {change:+.2f} ({change_pct:+.2f}%)]"
    )

    st.divider()

    col1, col2, col3, col4 = st.columns(4)
    col1.metric("P/E Ratio", f"{snapshot['peRatio']:.2f}")
    col2.metric("Price/Book", f"{snapshot['priceToBook']:.2f}")
    col3.metric("Dividend Yield", f"{snapshot['dividendYield']:.2f}%")
    col4.metric("Market Cap", format_market_cap(snapshot["marketCap"]))

    col5, col6, col7, col8 = st.columns(4)
    col5.metric("52W High", f"${snapshot['weekHigh52']:.2f}")
    col6.metric("52W Low", f"${snapshot['weekLow52']:.2f}")
    col7.metric("EPS", f"${snapshot['eps']:.2f}")
    col8.metric("Revenue YoY", f"{snapshot['revenueYoy']:.2f}%")

    st.caption(
        "Use the sidebar to view Valuation, Recommendation Trends, "
        "Related Stocks, or the Price Chart for this stock."
    )


if query:
    try:
        stock = search(query)
    except ApiError as e:
        st.error(str(e))
        st.stop()

    try:
        snapshot = get_snapshot(stock["ticker"])
    except ApiError as e:
        st.error(str(e))
        st.stop()

    # New search invalidates any valuation/trends/chart cached for the
    # previous ticker so other pages don't show stale data.
    if st.session_state.get("ticker") != stock["ticker"]:
        for key in ("valuation", "trends", "chart_data", "chart_range"):
            st.session_state.pop(key, None)

    st.session_state["ticker"] = stock["ticker"]
    st.session_state["stock"] = stock
    st.session_state["snapshot"] = snapshot

    render_snapshot(stock, snapshot)

elif "ticker" in st.session_state and "snapshot" in st.session_state:
    st.caption(f"Currently viewing **{st.session_state['ticker']}** — search again to change it.")
    render_snapshot(st.session_state["stock"], st.session_state["snapshot"])
