#!/usr/bin/env bash
# scripts/status.sh — read-only check: is the deployment currently up,
# and roughly how long/expensive has it been running.

set -euo pipefail

HOURLY_RATE_ESTIMATE="0.11"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AWS_DIR="$REPO_ROOT/aws"
STATE_FILE="$SCRIPT_DIR/.deployment_started_at"

echo "== AWS identity =="
aws sts get-caller-identity 2>&1 || echo "(not authenticated)"
echo ""

if [ -f "$STATE_FILE" ]; then
  STARTED_AT=$(cat "$STATE_FILE")
  NOW=$(date +%s)
  ELAPSED_SECONDS=$((NOW - STARTED_AT))
  ELAPSED_HOURS=$(echo "scale=2; $ELAPSED_SECONDS / 3600" | bc)
  ESTIMATED_COST=$(echo "scale=2; $ELAPSED_HOURS * $HOURLY_RATE_ESTIMATE" | bc)
  echo "== Local tracking says: deployment started $(date -r "$STARTED_AT") =="
  echo "Elapsed: ${ELAPSED_HOURS} hours — rough cost so far: \$${ESTIMATED_COST}"
else
  echo "== No local start timestamp found (not started via start.sh, or already stopped) =="
fi

echo ""
echo "== Actual Terraform state (ground truth, not just the timestamp file) =="
cd "$AWS_DIR"
RESOURCE_COUNT=$(terraform state list 2>/dev/null | wc -l | tr -d ' ')
echo "Resources currently tracked in state: $RESOURCE_COUNT"

if [ "$RESOURCE_COUNT" -gt 0 ]; then
  echo ""
  echo "Live URL:"
  terraform output alb_dns_name 2>/dev/null || echo "(not available)"
else
  echo "Nothing appears to be deployed right now."
fi
