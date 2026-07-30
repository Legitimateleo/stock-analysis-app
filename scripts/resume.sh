#!/usr/bin/env bash
# scripts/resume.sh — brings ECS services back up from desired-count 0.
# Much faster than start.sh/terraform apply since VPC, NAT Gateway, ALB,
# and the pipeline all stay intact — this only touches compute.
#
# Use this instead of start.sh when you paused with pause.sh and are
# coming back to demo again, rather than tearing everything down.

set -euo pipefail

EXPECTED_ACCOUNT_ID="963894087325"
CLUSTER="parallax-cluster"

echo "== Checking AWS identity =="
CALLER_JSON=$(aws sts get-caller-identity 2>&1) || {
  echo "ERROR: Could not authenticate to AWS."
  echo "$CALLER_JSON"
  exit 1
}
ACCOUNT_ID=$(echo "$CALLER_JSON" | grep -o '"Account": "[0-9]*"' | grep -o '[0-9]*')
ARN=$(echo "$CALLER_JSON" | grep -o '"Arn": "[^"]*"' | cut -d'"' -f4)
echo "Authenticated as: $ARN"

if [ "$ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]; then
  echo ""
  echo "!! REFUSING TO CONTINUE !! Expected account $EXPECTED_ACCOUNT_ID, got $ACCOUNT_ID."
  exit 1
fi
echo "Account confirmed — proceeding."
echo ""

echo "== Scaling ECS services up =="
aws ecs update-service --cluster "$CLUSTER" --service parallax-backend --desired-count 1 > /dev/null
aws ecs update-service --cluster "$CLUSTER" --service parallax-frontend --desired-count 1 > /dev/null

echo "== Waiting for both services to reach steady state =="
aws ecs wait services-stable --cluster "$CLUSTER" --services parallax-backend parallax-frontend
echo "    Both services running and passing health checks."

echo ""
echo "== Live URL (unchanged — ALB DNS name is stable across pause/resume) =="
aws elbv2 describe-load-balancers --names parallax-alb --query "LoadBalancers[0].DNSName" --output text | sed 's|^|http://|'

echo ""
echo "Reminder: NAT Gateway + ALB kept billing the whole time this was"
echo "paused (~\$4.90/day combined) — only Fargate compute actually"
echo "stopped. Resuming just adds that compute cost back."
