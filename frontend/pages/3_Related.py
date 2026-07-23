"""
pages/3_Related.py — use case 2: user clicks a related stock. Clicking
re-searches that ticker and switches back to Home to display it, the
same way clicking a related-stock chip in the desktop app triggers a
fresh search.
"""

import streamlit as st
from api_client import get_related, search, get_snapshot, ApiError
from theme import require_stock

st.set_page_config(page_title="Related Stocks — Parallax", page_icon="🔗", layout="wide")

ticker, stock = require_stock()

st.title(f"🔗 Related Stocks — {ticker}")
if stock.get("companyName"):
    st.caption(f"Other {stock.get('industry', '')} companies")

if st.button("Show Related Stocks", type="primary"):
    with st.spinner("Finding industry peers..."):
        try:
            related = get_related(ticker)
        except ApiError as e:
            st.error(str(e))
            st.stop()
    st.session_state["related"] = related

if "related" in st.session_state:
    related = st.session_state["related"]

    if not related:
        st.info("No other Fortune 500 companies found in this industry.")
    else:
        cols = st.columns(4)
        for i, peer in enumerate(related):
            with cols[i % 4]:
                if st.button(
                    f"{peer['ticker']}\n{peer['companyName']}",
                    key=f"related_{peer['ticker']}",
                    use_container_width=True,
                ):
                    with st.spinner(f"Loading {peer['ticker']}..."):
                        try:
                            new_stock = search(peer["ticker"])
                            new_snapshot = get_snapshot(peer["ticker"])
                        except ApiError as e:
                            st.error(str(e))
                            st.stop()

                    # Clear per-ticker cached data from the previous stock
                    for key in ("valuation", "trends", "related", "chart_data", "chart_range"):
                        st.session_state.pop(key, None)

                    st.session_state["ticker"] = new_stock["ticker"]
                    st.session_state["stock"] = new_stock
                    st.session_state["snapshot"] = new_snapshot

                    st.switch_page("app.py")
else:
    st.info("Press **Show Related Stocks** to see other companies in this industry.")
