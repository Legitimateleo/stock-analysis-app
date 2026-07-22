"""
pages/2_Trends.py — use case 4: user clicks the recommendation trends
button. Renders the same monthly analyst consensus data the desktop
app's RecommendationTrendsChart draws, as a stacked bar chart.
"""

import streamlit as st
import plotly.graph_objects as go
from api_client import get_trends, ApiError
from theme import require_stock, SIGNAL_COLORS

st.set_page_config(page_title="Trends — Parallax", page_icon="📊", layout="wide")

ticker, stock = require_stock()

st.title(f"📊 Analyst Recommendation Trends — {ticker}")
if stock.get("companyName"):
    st.caption(f"{stock['companyName']} · {stock.get('industry', '')}")

if st.button("Show Trends", type="primary"):
    with st.spinner("Fetching analyst data..."):
        try:
            trends = get_trends(ticker)
        except ApiError as e:
            st.error(str(e))
            st.stop()
    st.session_state["trends"] = trends

if "trends" in st.session_state:
    trends = st.session_state["trends"]

    if not trends:
        st.warning("No recommendation data available for this stock.")
    else:
        # Oldest first so the chart reads left-to-right chronologically
        trends_sorted = sorted(trends, key=lambda t: t["period"])
        periods = [t["period"] for t in trends_sorted]

        categories = [
            ("strongBuy", "Strong Buy", SIGNAL_COLORS["STRONG BUY"]),
            ("buy", "Buy", SIGNAL_COLORS["BUY"]),
            ("hold", "Hold", SIGNAL_COLORS["HOLD"]),
            ("sell", "Sell", SIGNAL_COLORS["SELL"]),
            ("strongSell", "Strong Sell", SIGNAL_COLORS["STRONG SELL"]),
        ]

        fig = go.Figure()
        for key, label, color in categories:
            fig.add_trace(go.Bar(
                name=label,
                x=periods,
                y=[t[key] for t in trends_sorted],
                marker_color=color,
            ))

        fig.update_layout(
            barmode="stack",
            template="plotly_dark",
            paper_bgcolor="rgba(0,0,0,0)",
            plot_bgcolor="rgba(0,0,0,0)",
            legend_title_text="Rating",
            xaxis_title="Month",
            yaxis_title="Number of Analysts",
            height=450,
        )

        st.plotly_chart(fig, use_container_width=True)
else:
    st.info("Press **Show Trends** to load analyst recommendation history.")
