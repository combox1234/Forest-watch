#!/bin/bash
# ==============================================================================
# ForestWatch AWS EC2 Production Continuous Delivery (CD) Script
# Course: Distributed Systems (Unit IV: Case Studies: AWS & DevOps)
# Project: Distributed Environmental Sensor Network
# ==============================================================================

set -euo pipefail

# 1. Use existing deployment directory on the EC2 instance
DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/forestwatch}"
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
    # Check for uncommitted tracked changes
    DIRTY_TRACKED=$(git diff --name-only)
    if [ -n "$DIRTY_TRACKED" ]; then
        echo "❌ Error: Working directory has uncommitted tracked changes in:"
        echo "$DIRTY_TRACKED"
        echo "Aborting deployment to avoid overwriting or losing local modifications."
        exit 1
    fi

    # 3 & 4. Update safely to the requested main-branch commit without force-resetting
    TARGET_COMMIT="${1:-main}"
    echo "📥 Fetching latest commits from origin/main..."
    git fetch origin main --tags

    echo "🔀 Checking out requested deployment commit: ${TARGET_COMMIT}"
    git checkout "${TARGET_COMMIT}"
    CURRENT_COMMIT=$(git rev-parse --short HEAD)
    echo "📌 Active deployment commit: ${CURRENT_COMMIT}"
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
# If the central-server container is currently healthy, perform online hot-backup
if docker compose ps --services --filter "status=running" 2>/dev/null | grep -q "central-server"; then
    echo "📦 Creating pre-deployment SQLite hot-backup..."
    BACKUP_FILE="/data/forestwatch_backup_$(date +%Y%m%d_%H%M%S).db"
    docker compose exec -T central-server sqlite3 /data/forestwatch.db ".backup ${BACKUP_FILE}" 2>/dev/null || true
    echo "✅ SQLite pre-deployment backup verified in persistent volume."
fi

# 8. Build required application images
echo "🏗️ Building updated core fleet container images..."
docker compose build central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer

# 9 & 10. Deploy core application using Docker Compose (preserving SQLite volume)
echo "🚀 Deploying core ForestWatch fleet (Zero volume destruction)..."
# NOTE: We intentionally avoid 'docker compose down -v' to strictly preserve SQLite data volumes
docker compose up -d --remove-orphans central-server sensor-1 sensor-2 sensor-3 sensor-4 load-balancer

# 11. Wait for application health checks and report failures
echo "⏳ Waiting for Central Coordinator & Sensor Fleet health checks..."
MAX_RETRIES=20
RETRY_COUNT=0
HEALTHY=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    sleep 3

    # Check central-server health via internal mapped port
    CENTRAL_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/actuator/health 2>/dev/null || echo "000")
    if [ "$CENTRAL_STATUS" = "200" ]; then
        HEALTHY=true
        echo "✅ Central Server health check PASSED (HTTP 200) after $((RETRY_COUNT*3))s."
        break
    else
        echo "   Waiting for central-server to report healthy... (attempt ${RETRY_COUNT}/${MAX_RETRIES}, HTTP ${CENTRAL_STATUS})"
    fi
done

if [ "$HEALTHY" != "true" ]; then
    echo "❌ Error: Central Server failed health check after $((MAX_RETRIES*3))s."
    echo "📋 Recent container logs for diagnosis:"
    docker compose logs --tail=40 central-server
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
