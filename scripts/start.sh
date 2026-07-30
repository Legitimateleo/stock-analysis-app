#!/usr/bin/env bash
# scripts/start.sh — spins up the AWS deployment via Terraform.
#
# Hard-blocks if the currently authenticated AWS account doesn't match
# the expected school account. This exists because of a real incident:
# `terraform apply` was once run against a work account by accident
# when a CLI session had the wrong credentials active. This check
# makes that mistake impossible to repeat silently.

set -euo pipefail

# ── Update this if your school account ID ever changes ──────────
EXPECTED_ACCOUNT_ID="963894087325"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AWS_DIR="$REPO_ROOT/aws"
STATE_FILE="$SCRIPT_DIR/.deployment_started_at"

echo "== Checking AWS identity =="
CALLER_JSON=$(aws sts get-caller-identity 2>&1) || {
  echo "ERROR: Could not authenticate to AWS at all."
  echo "$CALLER_JSON"
  echo ""
  echo "If you're in the project directory and using direnv, this"
  echo "usually means AWS_PROFILE isn't set correctly. Check .envrc."
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
  echo ""
  echo "This is exactly the mistake that happened once before — do not"
  echo "override this check without being certain which account you're"
  echo "actually pointed at."
  exit 1
fi

echo "Account confirmed — proceeding."
echo ""

cd "$AWS_DIR"

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
terraform output alb_dns_name

echo ""
echo "Reminder: NAT Gateway + ALB + Fargate bill continuously while this"
echo "is running (~\$0.11/hr per AWS_Cost_Estimate.md). Run stop.sh when"
echo "you're done, not just when you remember to."
