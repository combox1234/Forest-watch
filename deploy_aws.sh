#!/bin/bash
# ==============================================================================
# AWS EC2 1-Click Deployment Script
# Course: Distributed Systems (Unit IV: Case Studies: AWS & Containerization)
# Project: Distributed Environmental Sensor Network
# ==============================================================================

set -e

echo "=========================================================="
echo "🚀 Initializing Distributed Sensor Network on AWS EC2..."
echo "=========================================================="

# 1. Update OS and install Docker & Docker Compose if missing
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker Engine & Docker Compose..."
    sudo apt-get update -y
    sudo apt-get install -y ca-certificates curl gnupg lsb-release
    
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
    
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
      
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    sudo usermod -aG docker $USER
    echo "✅ Docker installed successfully."
fi

# 2. Check Docker daemon status
sudo systemctl start docker
sudo systemctl enable docker

# 3. Configure 2 GB Swap Memory (Essential for AWS Free Tier t2.micro 1GB RAM)
if [ $(swapon --show | wc -l) -le 1 ]; then
    echo "🧠 Configuring 2 GB Swap Memory for Free Tier t2.micro stability..."
    sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "✅ 2 GB Swap memory enabled."
fi

# 4. Environment settings
export AWS_REGION=${AWS_REGION:-"us-east-1"}
export AWS_S3_BUCKET=${AWS_S3_BUCKET:-"ds-environmental-telemetry-archive"}
export AWS_LAMBDA_FUNCTION_NAME=${AWS_LAMBDA_FUNCTION_NAME:-"EmergencyAlertDispatcher"}

echo "⚙️ Configuration:"
echo "   AWS Region: $AWS_REGION"
echo "   AWS S3 Bucket: $AWS_S3_BUCKET"
echo "   AWS Lambda Function: $AWS_LAMBDA_FUNCTION_NAME"

# 5. Build and run containers
echo "🏗️ Building and spinning up 5-Process Distributed Architecture..."
sudo docker compose down --remove-orphans || true
sudo docker compose up -d --build

# 6. Display status and public URL
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com || curl -s ifconfig.me || echo "YOUR-EC2-PUBLIC-IP")

echo "=========================================================="
echo "🎉 DEPLOYMENT COMPLETED SUCCESSFULLY ON AWS EC2!"
echo "=========================================================="
echo "Access your Distributed Network via browser:"
echo "👉 Central Coordinator & Dashboard: http://${PUBLIC_IP}:8099"
echo "👉 Sensor 1 Console (Cluster 1):    http://${PUBLIC_IP}:8101"
echo "👉 Sensor 2 Console (Cluster 1 CH): http://${PUBLIC_IP}:8102"
echo "👉 Sensor 3 Console (Cluster 2):    http://${PUBLIC_IP}:8103"
echo "👉 Sensor 4 Console (Cluster 2 CH): http://${PUBLIC_IP}:8104"
echo "👉 Grafana Observability Dashboard: http://${PUBLIC_IP}:3000 (admin/admin)"
echo "👉 Prometheus Metrics Server:       http://${PUBLIC_IP}:9090"
echo "=========================================================="
