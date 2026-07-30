#!/usr/bin/env bash
# scripts/stop.sh — tears down the AWS deployment via Terraform, and
# reports how long it was up and roughly what it cost.

set -euo pipefail

EXPECTED_ACCOUNT_ID="963894087325"
HOURLY_RATE_ESTIMATE="0.11" # from AWS_Cost_Estimate.md's Scenario 1 total

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AWS_DIR="$REPO_ROOT/aws"
STATE_FILE="$SCRIPT_DIR/.deployment_started_at"

echo "== Checking AWS identity =="
CALLER_JSON=$(aws sts get-caller-identity 2>&1) || {
  echo "ERROR: Could not authenticate to AWS at all."
  echo "$CALLER_JSON"
  exit 1
}

ACCOUNT_ID=$(echo "$CALLER_JSON" | grep -o '"Account": "[0-9]*"' | grep -o '[0-9]*')
ARN=$(echo "$CALLER_JSON" | grep -o '"Arn": "[^"]*"' | cut -d'"' -f4)

echo "Authenticated as: $ARN"
echo "Account: $ACCOUNT_ID"

if [ "$ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]; then
  echo ""
  echo "!! REFUSING TO CONTINUE !!"
  echo "Expected account $EXPECTED_ACCOUNT_ID (school), but currently"
  echo "authenticated as account $ACCOUNT_ID instead."
  exit 1
fi

echo "Account confirmed — proceeding."
echo ""

if [ -f "$STATE_FILE" ]; then
  STARTED_AT=$(cat "$STATE_FILE")
  NOW=$(date +%s)
  ELAPSED_SECONDS=$((NOW - STARTED_AT))
  ELAPSED_HOURS=$(echo "scale=2; $ELAPSED_SECONDS / 3600" | bc)
  ESTIMATED_COST=$(echo "scale=2; $ELAPSED_HOURS * $HOURLY_RATE_ESTIMATE" | bc)
  echo "Deployment was up for approximately ${ELAPSED_HOURS} hours."
  echo "Rough estimated cost: \$${ESTIMATED_COST} (excludes flat monthly"
  echo "charges like CodePipeline/Secrets Manager — see AWS_Cost_Estimate.md)"
  echo ""
else
  echo "No start timestamp found — was this started via start.sh?"
  echo "Proceeding with destroy anyway."
  echo ""
fi

cd "$AWS_DIR"

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
