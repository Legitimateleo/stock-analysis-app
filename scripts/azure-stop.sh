#!/usr/bin/env bash
# scripts/stop.sh — tears down the Azure deployment via Terraform, and
# reports how long it was up and roughly what it cost.

set -euo pipefail

EXPECTED_SUBSCRIPTION_ID="d5fde73e-9028-4e91-960d-faef27f6ad22" # CSUSM tenant, confirmed via az account show
HOURLY_RATE_ESTIMATE="0.05" # placeholder — replace once AZURE_Cost_Estimate.md's real B1 Plan rate is confirmed

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AZURE_DIR="$REPO_ROOT/azure"
STATE_FILE="$SCRIPT_DIR/.deployment_started_at_azure"

echo "== Checking Azure identity =="
ACCOUNT_JSON=$(az account show 2>&1) || {
  echo "ERROR: Not logged in to Azure at all."
  echo "$ACCOUNT_JSON"
  exit 1
}

SUBSCRIPTION_ID=$(echo "$ACCOUNT_JSON" | grep -o '"id": *"[^"]*"' | head -1 | cut -d'"' -f4)
SUBSCRIPTION_NAME=$(echo "$ACCOUNT_JSON" | grep -o '"name": *"[^"]*"' | head -1 | cut -d'"' -f4)

echo "Authenticated to subscription: $SUBSCRIPTION_NAME"
echo "Subscription ID: $SUBSCRIPTION_ID"

if [ "$SUBSCRIPTION_ID" != "$EXPECTED_SUBSCRIPTION_ID" ]; then
  echo ""
  echo "!! REFUSING TO CONTINUE !!"
  echo "Expected subscription $EXPECTED_SUBSCRIPTION_ID, but currently"
  echo "active is $SUBSCRIPTION_ID ($SUBSCRIPTION_NAME) instead."
  exit 1
fi

echo "Subscription confirmed — proceeding."
echo ""

if [ -f "$STATE_FILE" ]; then
  STARTED_AT=$(cat "$STATE_FILE")
  NOW=$(date +%s)
  ELAPSED_SECONDS=$((NOW - STARTED_AT))
  ELAPSED_HOURS=$(echo "scale=2; $ELAPSED_SECONDS / 3600" | bc)
  ESTIMATED_COST=$(echo "scale=2; $ELAPSED_HOURS * $HOURLY_RATE_ESTIMATE" | bc)
  echo "Deployment was up for approximately ${ELAPSED_HOURS} hours."
  echo "Rough estimated cost: \$${ESTIMATED_COST} (placeholder rate —"
  echo "update HOURLY_RATE_ESTIMATE once AZURE_Cost_Estimate.md exists)"
  echo ""
else
  echo "No start timestamp found — was this started via start.sh?"
  echo "Proceeding with destroy anyway."
  echo ""
fi

cd "$AZURE_DIR"

echo "== Running terraform plan to confirm what will be destroyed =="
terraform plan -destroy

echo ""
read -p "Confirm destroy of everything above? Type 'yes' to proceed: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted — nothing was destroyed."
  exit 0
fi

echo "== Destroying =="
terraform destroy -auto-approve

rm -f "$STATE_FILE"
echo ""
echo "== Torn down. Nothing should be billing from this deployment now. =="
