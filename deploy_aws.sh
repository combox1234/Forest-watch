#!/bin/bash
# ==============================================================================
# ForestWatch AWS EC2 Production Continuous Delivery (CD) Script
# Course: Distributed Systems (Unit IV: Case Studies: AWS & DevOps)
# Project: Distributed Environmental Sensor Network
# ==============================================================================

set -euo pipefail

# 1. Use existing deployment directory on the EC2 instance
DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/Forest-watch}"
if [ ! -d "$DEPLOY_DIR" ]; then
    DEPLOY_DIR="$(pwd)"
fi

echo "=========================================================="
echo "🚀 ForestWatch Automated Deployment Triggered"
echo "📂 Target Directory: ${DEPLOY_DIR}"
echo "⏱️  Timestamp: $(date -u +"%Y-%m-%d %H:%M:%SZ")"
echo "=========================================================="

cd "${DEPLOY_DIR}"

# 2. Verify that the Git working tree is clean before updating
echo "🔍 Checking Git working tree status..."
if [ -d .git ]; then
    # Check for unstaged modifications in tracked files
    DIRTY_UNSTAGED=$(git diff --name-only)
    # Check for staged uncommitted changes
    DIRTY_STAGED=$(git diff --cached --name-only)
    # Check for untracked files not ignored by .gitignore
    UNTRACKED_FILES=$(git ls-files --others --exclude-standard)

    if [ -n "$DIRTY_UNSTAGED" ] || [ -n "$DIRTY_STAGED" ] || [ -n "$UNTRACKED_FILES" ]; then
        echo "❌ Error: Working directory has uncommitted or untracked changes."
        echo "Aborting deployment to avoid overwriting or losing local modifications."
        if [ -n "$DIRTY_UNSTAGED" ]; then
            echo "   Modified tracked files (unstaged):"
            echo "$DIRTY_UNSTAGED" | sed 's/^/     - /'
        fi
        if [ -n "$DIRTY_STAGED" ]; then
            echo "   Staged changes:"
            echo "$DIRTY_STAGED" | sed 's/^/     - /'
        fi
        if [ -n "$UNTRACKED_FILES" ]; then
            echo "   Untracked files (not in .gitignore):"
            echo "$UNTRACKED_FILES" | sed 's/^/     - /'
        fi
        echo "Please commit, stash, or clean these changes before running deployment."
        exit 1
    fi

    # 3 & 4. Update safely to the requested commit without force-resetting
    TARGET_COMMIT="${1:-main}"
    echo "📥 Fetching latest commits and tags from remote origin..."
    git fetch origin --tags

    # If the target commit cannot be resolved from existing local/fetched refs, attempt direct fetch
    if ! git rev-parse --verify "${TARGET_COMMIT}^{commit}" >/dev/null 2>&1; then
        echo "   Commit '${TARGET_COMMIT}' not found in standard fetched refs. Attempting direct ref fetch..."
        git fetch origin "${TARGET_COMMIT}" 2>/dev/null || true
    fi

    # Strictly verify that the requested target commit can be resolved
    if ! git rev-parse --verify "${TARGET_COMMIT}^{commit}" >/dev/null 2>&1; then
        echo "❌ Error: Unable to resolve deployment target commit: '${TARGET_COMMIT}'."
        echo "The requested commit does not exist locally or in remote 'origin'."
        exit 1
    fi

    echo "🔀 Checking out requested deployment commit: ${TARGET_COMMIT}"
    if [ "${TARGET_COMMIT}" = "main" ]; then
        git checkout main
        git merge --ff-only origin/main 2>/dev/null || git checkout origin/main
    else
        git checkout "${TARGET_COMMIT}"
    fi

    RESOLVED_COMMIT=$(git rev-parse HEAD)
    CURRENT_COMMIT=$(git rev-parse --short HEAD)
    echo "📌 Active deployment commit: ${CURRENT_COMMIT} (${RESOLVED_COMMIT})"
else
    echo "ℹ️  Not a git repository directory. Skipping git fetch/checkout."
fi

# 5. Verify required secrets are configured in .env without printing values
echo "🔐 Verifying environment configuration and secrets..."
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "⚠️  .env file not found. Initializing .env from template .env.example..."
        cp .env.example .env
        chmod 600 .env
    else
        echo "❌ Error: Neither .env nor .env.example found in ${DEPLOY_DIR}."
        exit 1
    fi
fi

# Check required application secret
if ! grep -q "^FORESTWATCH_SECURITY_SERVICE_KEY=" .env || \
   grep -q "^FORESTWATCH_SECURITY_SERVICE_KEY=change-this" .env; then
    echo "⚠️  Warning: FORESTWATCH_SECURITY_SERVICE_KEY is unset or using default template value."
else
    echo "✅ FORESTWATCH_SECURITY_SERVICE_KEY is configured."
fi

# 6. Validate Docker Compose configuration before deployment
echo "🐳 Validating Docker Compose configuration..."
if ! docker compose config -q; then
    echo "❌ Error: 'docker compose config' validation failed. Check syntax in docker-compose.yml and .env."
    exit 1
fi
echo "✅ Docker Compose configuration syntax is valid."

# 7 & D.1. SQLite Persistence & Safe Pre-Deployment Backup
echo "💾 Checking SQLite database persistence..."
mkdir -p data

# If central-server is currently running and has an active database, perform verified online hot-backup
if docker compose ps --services --filter "status=running" 2>/dev/null | grep -q "central-server"; then
    if docker compose exec -T central-server test -f /data/forestwatch.db 2>/dev/null; then
        echo "📦 Creating pre-deployment SQLite hot-backup..."
        BACKUP_FILE="/data/forestwatch_backup_$(date +%Y%m%d_%H%M%S).db"

        # Verify sqlite3 CLI is available inside the container
        if ! docker compose exec -T central-server which sqlite3 >/dev/null 2>&1; then
            echo "❌ Error: sqlite3 CLI is not installed inside the running central-server container."
            echo "Cannot execute reliable pre-deployment backup. Aborting deployment before replacing containers to protect data."
            exit 1
        fi

        # Execute safe online hot backup
        if ! docker compose exec -T central-server sqlite3 /data/forestwatch.db ".backup '${BACKUP_FILE}'"; then
            echo "❌ Error: Pre-deployment SQLite backup failed."
            echo "Aborting deployment before replacing running containers to preserve database integrity."
            exit 1
        fi

        # Verify backup file was created and is non-empty
        if ! docker compose exec -T central-server test -s "${BACKUP_FILE}"; then
            echo "❌ Error: Pre-deployment SQLite backup file is empty or missing: ${BACKUP_FILE}"
            exit 1
        fi

        echo "✅ SQLite pre-deployment backup verified in persistent volume (${BACKUP_FILE})."
        echo "   ℹ️ Note: This is an on-disk volume snapshot before container update, not an off-site disaster-recovery copy."
    else
        echo "ℹ️  No existing database file at /data/forestwatch.db. Skipping pre-deployment backup."
    fi
else
    echo "ℹ️  Central server is not currently running. Skipping online container hot-backup."
fi

# 8. Build required application images
echo "🏗️ Building updated core fleet container images..."
if ! docker compose build central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer; then
    echo "❌ Error: Docker build failed for core fleet images."
    echo "Aborting deployment without modifying running containers or persistent database."
    exit 1
fi

# 9 & 10. Deploy core application using Docker Compose (preserving SQLite volume)
echo "🚀 Deploying core ForestWatch fleet (Zero volume destruction)..."
# NOTE: We intentionally avoid 'docker compose down -v' to strictly preserve SQLite data volumes
if ! docker compose up -d --remove-orphans central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer; then
    echo "❌ Error: 'docker compose up' failed to deploy core fleet."
    echo "Aborting deployment. Persistent SQLite volume remains intact."
    exit 1
fi

# 11. Wait for application health checks and report failures
echo "⏳ Waiting for Central Coordinator & Sensor Fleet health checks..."
MAX_RETRIES=20
RETRY_COUNT=0
CENTRAL_HEALTHY=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    sleep 3

    # Check central-server health via internal mapped port
    CENTRAL_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/actuator/health 2>/dev/null || echo "000")
    if [ "$CENTRAL_STATUS" = "200" ]; then
        CENTRAL_HEALTHY=true
        echo "✅ Central Server health check PASSED (HTTP 200) after $((RETRY_COUNT*3))s."
        break
    else
        echo "   Waiting for central-server to report healthy... (attempt ${RETRY_COUNT}/${MAX_RETRIES}, HTTP ${CENTRAL_STATUS})"
    fi
done

if [ "$CENTRAL_HEALTHY" != "true" ]; then
    echo "❌ Error: Central Server failed health check after $((MAX_RETRIES*3))s."
    echo "📋 Recent container logs for diagnosis:"
    docker compose logs --tail=40 central-server
    exit 1
fi

# Verify Sensor Fleet Health (S1-S4) via established Spring Boot Actuator endpoints
echo "⏳ Verifying Sensor Node health (S1-S4)..."
SENSOR_PORTS=(8101 8102 8103 8104)
SENSOR_NODES=("sensor-1 (S1)" "sensor-2 (S2)" "sensor-3 (S3)" "sensor-4 (S4)")
SENSOR_SERVICES=("sensor-1" "sensor-2" "sensor-3" "sensor-4")
SENSOR_FAILURES=0

for idx in "${!SENSOR_PORTS[@]}"; do
    PORT="${SENSOR_PORTS[$idx]}"
    NODE_NAME="${SENSOR_NODES[$idx]}"
    SERVICE_NAME="${SENSOR_SERVICES[$idx]}"

    NODE_HEALTHY=false
    NODE_RETRY=0
    MAX_NODE_RETRIES=40

    while [ $NODE_RETRY -lt $MAX_NODE_RETRIES ]; do
        NODE_RETRY=$((NODE_RETRY+1))
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" 2>/dev/null || echo "000")
        if [ "$HTTP_CODE" = "200" ]; then
            NODE_HEALTHY=true
            echo "✅ ${NODE_NAME} health check PASSED (HTTP 200 on port ${PORT})."
            break
        fi
        sleep 3
    done

    if [ "$NODE_HEALTHY" != "true" ]; then
        echo "❌ Error: ${NODE_NAME} failed health check (HTTP ${HTTP_CODE} on port ${PORT})."
        echo "📋 Recent container logs for ${SERVICE_NAME}:"
        docker compose logs --tail=30 "${SERVICE_NAME}"
        SENSOR_FAILURES=$((SENSOR_FAILURES+1))
    fi
done

if [ $SENSOR_FAILURES -gt 0 ]; then
    echo "❌ Error: ${SENSOR_FAILURES} sensor node(s) failed health verification."
    echo "Halting deployment with non-zero exit code. Containers are left running for inspection (zero rollback/data loss)."
    exit 1
fi

# 12. Display useful deployment status without printing credentials
echo "=========================================================="
echo "🎉 DEPLOYMENT SUCCEEDED"
echo "=========================================================="
echo "Active Fleet Containers:"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

PUBLIC_IP=$(curl -s -m 3 http://checkip.amazonaws.com 2>/dev/null || curl -s -m 3 ifconfig.me 2>/dev/null || echo "localhost")
echo ""
echo "🌐 Public Application Access (NGINX Load Balancer Port 80):"
echo "👉 Operations Dashboard: http://${PUBLIC_IP}/"
echo "👉 Sensor Consoles:     http://${PUBLIC_IP}/sensor1/ .. /sensor4/"
echo "=========================================================="
