#!/usr/bin/env bash
# scripts/pause.sh — scales ECS services to 0 without tearing down the
# VPC/NAT Gateway/ALB/pipeline/IAM users. Faster to reverse than
# stop.sh's full destroy, but NOT free — NAT Gateway and ALB keep
# billing the whole time regardless of task count.
#
# Use stop.sh instead if you don't know when you're coming back —
# use this if you're demoing again soon and want fast resume.

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

echo "== Scaling ECS services to 0 =="
aws ecs update-service --cluster "$CLUSTER" --service parallax-backend --desired-count 0 > /dev/null
aws ecs update-service --cluster "$CLUSTER" --service parallax-frontend --desired-count 0 > /dev/null
echo "    Both services scaling down. Fargate compute billing stops once tasks fully drain (~30-60s)."

echo ""
echo "== NOT stopped: NAT Gateway (~\$32.85/mo) and ALB (~\$16.43/mo) =="
echo "These keep billing regardless of task count — pausing only stops"
echo "Fargate compute (~\$18/mo combined for both services)."
echo ""
echo "If you don't know when you're coming back, run stop.sh instead for"
echo "a full \$0 teardown. Run resume.sh to bring compute back up quickly."
