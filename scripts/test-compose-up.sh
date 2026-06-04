#!/usr/bin/env bash
# Half-automated E2E for TC-DRT-001 (compose up healthy) + TC-DRT-002 (no MySQL conn errors).
# Run from repo root. Requires: docker, docker compose, curl, jq.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-180}"
BACKEND_HOST_PORT="${RAINIER_BACKEND_HOST_PORT:-8080}"
FRONTEND_HOST_PORT="${RAINIER_FRONTEND_HOST_PORT:-80}"

cleanup() {
  echo "==> Tearing down: docker compose down -v"
  docker compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> docker compose up -d --build"
docker compose up -d --build

echo "==> Waiting up to ${TIMEOUT_SECONDS}s for mysql/backend/frontend to become healthy..."
deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
all_healthy=0
while (( $(date +%s) < deadline )); do
  # docker compose ps --format json emits NDJSON (one object per service).
  unhealthy_count=$(
    docker compose ps --format json \
      | jq -s '[.[] | select((.Health // "") != "healthy")] | length'
  )
  if [[ "$unhealthy_count" == "0" ]]; then
    all_healthy=1
    break
  fi
  sleep 3
done

if [[ "$all_healthy" -ne 1 ]]; then
  echo "❌ Services did not all become healthy within ${TIMEOUT_SECONDS}s. Current state:"
  docker compose ps
  echo "--- backend logs (tail 80) ---"
  docker compose logs --tail 80 backend || true
  echo "--- mysql logs (tail 40) ---"
  docker compose logs --tail 40 mysql || true
  exit 1
fi

echo "✅ All services healthy"

echo "==> Probing GET http://localhost:${BACKEND_HOST_PORT}/api/health"
curl -fsS "http://localhost:${BACKEND_HOST_PORT}/api/health" | tee /dev/stderr | grep -q '"status":"UP"'

echo "==> Probing HEAD http://localhost:${FRONTEND_HOST_PORT}/"
curl -fsSI "http://localhost:${FRONTEND_HOST_PORT}/" | head -1 | grep -q "200"

echo "✅ TC-DRT-001 PASSED"

echo "==> Verifying backend log contains no MySQL 'Communications link failure'"
if docker compose logs backend | grep -q "Communications link failure"; then
  echo "❌ Detected 'Communications link failure' in backend log"
  exit 1
fi
echo "✅ TC-DRT-002 PASSED (no MySQL connection failures)"

echo "✅ All compose E2E checks passed."
