#!/bin/bash
# ==============================================================================
# ForestWatch AWS EC2 Production Continuous Delivery (CD) Script
# Fix: allow up to 120 seconds for each Spring Boot sensor to become healthy.
# ==============================================================================

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/Forest-watch}"
if [ ! -d "$DEPLOY_DIR" ]; then
    DEPLOY_DIR="$(pwd)"
fi

echo "=========================================================="
echo "ForestWatch Automated Deployment Triggered"
echo "Target Directory: ${DEPLOY_DIR}"
echo "Timestamp: $(date -u +"%Y-%m-%d %H:%M:%SZ")"
echo "=========================================================="

cd "$DEPLOY_DIR"

# Verify the Git working tree is clean before updating.
if [ -d .git ]; then
    DIRTY_UNSTAGED=$(git diff --name-only)
    DIRTY_STAGED=$(git diff --cached --name-only)
    UNTRACKED_FILES=$(git ls-files --others --exclude-standard)

    if [ -n "$DIRTY_UNSTAGED" ] || [ -n "$DIRTY_STAGED" ] || [ -n "$UNTRACKED_FILES" ]; then
        echo "ERROR: Working directory has uncommitted or untracked changes."
        [ -z "$DIRTY_UNSTAGED" ] || { echo "Modified files:"; printf '%s\n' "$DIRTY_UNSTAGED"; }
        [ -z "$DIRTY_STAGED" ] || { echo "Staged files:"; printf '%s\n' "$DIRTY_STAGED"; }
        [ -z "$UNTRACKED_FILES" ] || { echo "Untracked files:"; printf '%s\n' "$UNTRACKED_FILES"; }
        exit 1
    fi

    TARGET_COMMIT="${1:-main}"
    echo "Fetching origin..."
    git fetch origin --tags

    if ! git rev-parse --verify "${TARGET_COMMIT}^{commit}" >/dev/null 2>&1; then
        git fetch origin "$TARGET_COMMIT" 2>/dev/null || true
    fi
    if ! git rev-parse --verify "${TARGET_COMMIT}^{commit}" >/dev/null 2>&1; then
        echo "ERROR: Unable to resolve deployment target commit: ${TARGET_COMMIT}"
        exit 1
    fi

    if [ "$TARGET_COMMIT" = "main" ]; then
        git checkout main
        git merge --ff-only origin/main 2>/dev/null || git checkout origin/main
    else
        git checkout "$TARGET_COMMIT"
    fi
    echo "Active commit: $(git rev-parse --short HEAD)"
else
    echo "Not a Git repository; skipping Git update."
fi

# Check environment configuration without printing secret values.
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "WARNING: .env missing; creating it from .env.example."
        cp .env.example .env
        chmod 600 .env
    else
        echo "ERROR: Neither .env nor .env.example exists."
        exit 1
    fi
fi

if ! grep -q '^FORESTWATCH_SECURITY_SERVICE_KEY=' .env ||
   grep -q '^FORESTWATCH_SECURITY_SERVICE_KEY=change-this' .env; then
    echo "WARNING: FORESTWATCH_SECURITY_SERVICE_KEY is unset or still has the template value."
else
    echo "FORESTWATCH_SECURITY_SERVICE_KEY is configured."
fi

echo "Validating Docker Compose configuration..."
docker compose config -q

# Preserve existing SQLite data and take an online backup before replacing containers.
mkdir -p data
if docker compose ps --services --filter "status=running" 2>/dev/null | grep -q "central-server"; then
    if docker compose exec -T central-server test -f /data/forestwatch.db 2>/dev/null; then
        BACKUP_FILE="/data/forestwatch_backup_$(date +%Y%m%d_%H%M%S).db"
        if ! docker compose exec -T central-server which sqlite3 >/dev/null 2>&1; then
            echo "ERROR: sqlite3 CLI is unavailable inside central-server; refusing to deploy without a verified backup."
            exit 1
        fi
        docker compose exec -T central-server sqlite3 /data/forestwatch.db ".backup '${BACKUP_FILE}'"
        docker compose exec -T central-server test -s "$BACKUP_FILE"
        echo "SQLite backup verified: ${BACKUP_FILE}"
    else
        echo "No existing SQLite database found; skipping online backup."
    fi
else
    echo "central-server is not running; skipping online backup."
fi

echo "Building ForestWatch images..."
docker compose build central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer

echo "Starting ForestWatch containers..."
docker compose up -d --remove-orphans central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer

echo "Waiting for central-server health (up to 60 seconds)..."
CENTRAL_HEALTHY=false
for attempt in $(seq 1 20); do
    HTTP_CODE=$(curl -sS --connect-timeout 2 --max-time 5 -o /dev/null -w '%{http_code}' http://localhost:8099/actuator/health 2>/dev/null || true)
    if [ "$HTTP_CODE" = "200" ]; then
        CENTRAL_HEALTHY=true
        echo "Central server healthy (HTTP 200) after approximately $((attempt * 3)) seconds."
        break
    fi
    echo "Central server not ready yet (attempt ${attempt}/20; HTTP ${HTTP_CODE:-000})."
    sleep 3
done

if [ "$CENTRAL_HEALTHY" != "true" ]; then
    echo "ERROR: central-server did not become healthy within 60 seconds."
    docker compose logs --tail=40 central-server || true
    exit 1
fi

echo "Checking sensor health (up to 120 seconds per sensor)..."
SENSOR_PORTS=(8101 8102 8103 8104)
SENSOR_NODES=("sensor-1 (S1)" "sensor-2 (S2)" "sensor-3 (S3)" "sensor-4 (S4)")
SENSOR_SERVICES=("sensor-1" "sensor-2" "sensor-3" "sensor-4")
SENSOR_FAILURES=0

for idx in "${!SENSOR_PORTS[@]}"; do
    PORT="${SENSOR_PORTS[$idx]}"
    NODE_NAME="${SENSOR_NODES[$idx]}"
    SERVICE_NAME="${SENSOR_SERVICES[$idx]}"
    NODE_HEALTHY=false
    HTTP_CODE="000"

    for attempt in $(seq 1 40); do
        HTTP_CODE=$(curl -sS --connect-timeout 2 --max-time 5 -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/actuator/health" 2>/dev/null || true)
        if [ "$HTTP_CODE" = "200" ]; then
            NODE_HEALTHY=true
            echo "${NODE_NAME} health check PASSED (HTTP 200 on port ${PORT}) after approximately $((attempt * 3)) seconds."
            break
        fi
        echo "${NODE_NAME} is not ready yet (attempt ${attempt}/40; HTTP ${HTTP_CODE:-000})."
        sleep 3
    done

    if [ "$NODE_HEALTHY" != "true" ]; then
        echo "ERROR: ${NODE_NAME} failed health check on port ${PORT} after up to 120 seconds (HTTP ${HTTP_CODE:-000})."
        docker compose logs --tail=50 "$SERVICE_NAME" || true
        SENSOR_FAILURES=$((SENSOR_FAILURES + 1))
    fi
done

if [ "$SENSOR_FAILURES" -gt 0 ]; then
    echo "ERROR: ${SENSOR_FAILURES} sensor node(s) failed health verification."
    echo "Containers are left running for inspection; persistent volumes were not removed."
    exit 1
fi

echo "=========================================================="
echo "DEPLOYMENT SUCCEEDED"
echo "=========================================================="
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

PUBLIC_IP=$(curl -sS -m 3 http://checkip.amazonaws.com 2>/dev/null || curl -sS -m 3 https://ifconfig.me 2>/dev/null || echo "localhost")
echo
echo "Public Application Access (NGINX Load Balancer Port 80):"
echo "Operations Dashboard: http://${PUBLIC_IP}/"
echo "Sensor Consoles:      http://${PUBLIC_IP}/sensor1/ .. /sensor4/"
echo "=========================================================="
