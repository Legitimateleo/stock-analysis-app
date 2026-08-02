#!/usr/bin/env bash
# scripts/start.sh — spins up the Azure deployment via Terraform.
#
# Hard-blocks if the currently active Azure subscription doesn't match
# the expected school subscription. This exists for the same reason
# AWS's start.sh does: an account/subscription mix-up already happened
# twice during this project's AWS phase. Applying that same discipline
# to Azure from the start, not after an incident here too.

set -euo pipefail

# ── Fill this in after running `az account show` once — see README ──
EXPECTED_SUBSCRIPTION_ID="d5fde73e-9028-4e91-960d-faef27f6ad22" # CSUSM tenant, confirmed via az account show

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AZURE_DIR="$REPO_ROOT/azure"
STATE_FILE="$SCRIPT_DIR/.deployment_started_at_azure"

echo "== Checking Azure identity =="
ACCOUNT_JSON=$(az account show 2>&1) || {
  echo "ERROR: Not logged in to Azure at all."
  echo "$ACCOUNT_JSON"
  echo ""
  echo "Run 'az login' first."
  exit 1
}

SUBSCRIPTION_ID=$(echo "$ACCOUNT_JSON" | grep -o '"id": *"[^"]*"' | head -1 | cut -d'"' -f4)
SUBSCRIPTION_NAME=$(echo "$ACCOUNT_JSON" | grep -o '"name": *"[^"]*"' | head -1 | cut -d'"' -f4)

echo "Authenticated to subscription: $SUBSCRIPTION_NAME"
echo "Subscription ID: $SUBSCRIPTION_ID"

if [ "$SUBSCRIPTION_ID" != "$EXPECTED_SUBSCRIPTION_ID" ]; then
  echo ""
  echo "!! REFUSING TO CONTINUE !!"
  echo "Expected subscription $EXPECTED_SUBSCRIPTION_ID (school), but"
  echo "currently active is $SUBSCRIPTION_ID ($SUBSCRIPTION_NAME) instead."
  echo ""
  echo "If you have multiple subscriptions, run:"
  echo "  az account set --subscription <name-or-id>"
  echo "and try again. Do not override this check without being certain"
  echo "which subscription you're actually pointed at."
  exit 1
fi

echo "Subscription confirmed — proceeding."
echo ""

cd "$AZURE_DIR"

echo "== Running terraform plan first =="
terraform plan

echo ""
read -p "Does the plan above look correct? Type 'yes' to apply: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted — nothing was applied."
  exit 0
fi

echo "== Applying =="
terraform apply -auto-approve

date +%s > "$STATE_FILE"

echo ""
echo "== Deployment is up =="
terraform output frontend_url
terraform output backend_url

echo ""
echo "Reminder: the App Service Plan bills hourly the whole time it"
echo "exists, regardless of whether the apps are actively serving"
echo "traffic (see AZURE_Cost_Estimate.md once written) — unlike AWS,"
echo "there's no separate 'pause the compute, keep the LB' option here,"
echo "since App Service has no equivalent of Fargate's per-task billing."
echo "Run stop.sh when you're done, not just when you remember to."
