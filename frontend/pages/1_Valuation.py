"""
pages/1_Valuation.py — use case 3: user presses the valuation button.
"""

import streamlit as st
from api_client import get_valuation, ApiError
from theme import require_stock, signal_badge

st.set_page_config(page_title="Valuation — Parallax", page_icon="💰", layout="wide")

ticker, stock = require_stock()

st.title(f"💰 Valuation — {ticker}")
if stock.get("companyName"):
    st.caption(f"{stock['companyName']} · {stock.get('industry', '')}")

if st.button("Calculate Valuation", type="primary"):
    with st.spinner("Scoring..."):
        try:
            result = get_valuation(ticker)
        except ApiError as e:
            st.error(str(e))
            st.stop()

    st.session_state["valuation"] = result

if "valuation" in st.session_state:
    result = st.session_state["valuation"]
    score = result["score"]
    signal = result["signal"]

    col1, col2 = st.columns([1, 2])
    with col1:
        st.metric("Composite Score", f"{score:.2f} / 10")
    with col2:
        st.markdown("**Signal**")
        st.markdown(signal_badge(signal), unsafe_allow_html=True)

    st.progress(min(max(score / 10, 0.0), 1.0))

    st.caption(
        "Score averages three factors: forward P/E vs. market average, "
        "price position within the 52-week range, and P/E relative to "
        "the stock's sector average."
    )
else:
    st.info("Press **Calculate Valuation** to score this stock.")
