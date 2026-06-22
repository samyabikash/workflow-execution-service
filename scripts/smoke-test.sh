#!/usr/bin/env bash
#
# End-to-end smoke test for the Workflow Execution Service.
#
# It:
#   1. Creates a workflow that demonstrates transform, delay, and context propagation.
#   2. Starts an (asynchronous) execution and captures the execution id.
#   3. Polls execution state until it reaches a terminal status.
#   4. Prints the full execution history, including the final workflow context.
#
# Prerequisites: the service must be running (e.g. ./mvnw spring-boot:run).
#
# Usage:
#   ./scripts/smoke-test.sh [BASE_URL]
#   BASE_URL defaults to http://localhost:8080

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
API="${BASE_URL}/api/v1"
WORKFLOW_ID="smoke-$(date +%s)"

echo "==> Using base URL: ${BASE_URL}"
echo "==> Workflow id:    ${WORKFLOW_ID}"

echo
echo "==> 1. Creating workflow (transform -> delay -> validate -> approve)"
curl -sS -X POST "${API}/workflows" \
  -H "Content-Type: application/json" \
  -d "{
        \"workflowId\": \"${WORKFLOW_ID}\",
        \"steps\": [
          { \"name\": \"transform\", \"input\": { \"sourceKey\": \"contractId\", \"contractId\": \"  c-42  \", \"operation\": \"trim\", \"targetKey\": \"contractId\" } },
          { \"name\": \"delay\", \"input\": { \"durationMillis\": 1500 } },
          { \"name\": \"validate\", \"input\": { \"contractId\": \"c-42\" } },
          { \"name\": \"approve\" }
        ]
      }"
echo

echo
echo "==> 2. Starting execution (expect HTTP 202 Accepted)"
EXEC_RESPONSE=$(curl -sS -X POST "${API}/workflows/${WORKFLOW_ID}/executions")
echo "${EXEC_RESPONSE}"
EXECUTION_ID=$(echo "${EXEC_RESPONSE}" | sed -n 's/.*"executionId"[ ]*:[ ]*"\([^"]*\)".*/\1/p')
echo "==> Execution id: ${EXECUTION_ID}"

echo
echo "==> 3. Polling execution state until terminal"
for _ in $(seq 1 30); do
  STATE=$(curl -sS "${API}/executions/${EXECUTION_ID}/state")
  STATUS=$(echo "${STATE}" | sed -n 's/.*"status"[ ]*:[ ]*"\([^"]*\)".*/\1/p')
  echo "    state: ${STATE}"
  case "${STATUS}" in
    COMPLETED|FAILED|CANCELLED) break ;;
  esac
  sleep 1
done

echo
echo "==> 4. Final execution history (includes finalContext)"
curl -sS "${API}/executions/${EXECUTION_ID}"
echo
