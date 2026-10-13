#!/bin/bash
# ==============================================================================
# Kubernetes & Load Balancer Automated Deployment Script
# Course: Distributed Systems (Unit IV: Case Studies: Kubernetes & Load Balancing)
# Project: Distributed Environmental Sensor Network
# ==============================================================================

set -e

echo "=========================================================="
echo "☸️  Deploying Distributed Sensor Network to Kubernetes..."
echo "=========================================================="

# 1. Check for kubectl
if ! command -v kubectl &> /dev/null; then
    echo "❌ Error: 'kubectl' CLI not found. Please install kubectl or start Minikube / Docker Desktop / Kind."
    exit 1
fi

echo "🔍 Connected Kubernetes Cluster:"
kubectl cluster-info || true

# 2. Build local Docker images if running on Minikube / Docker Desktop / Kind
if [ "$1" == "--build" ] || [ "$BUILD_IMAGES" == "true" ]; then
    echo "🔨 Building Docker images for Kubernetes cluster..."
    docker build -t central-server:1.0.0 -f docker/Dockerfile.central .
    docker build -t sensor-server:1.0.0 -f docker/Dockerfile.sensor .
    
    # If using Minikube, load images directly into Minikube VM
    if command -v minikube &> /dev/null && minikube status &> /dev/null; then
        echo "📥 Loading images into Minikube..."
        minikube image load central-server:1.0.0
        minikube image load sensor-server:1.0.0
    fi
fi

# 3. Apply manifests in sequence
echo "📦 Applying Kubernetes Manifests..."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-config.yaml
kubectl apply -f k8s/02-postgres.yaml
kubectl apply -f k8s/03-central-server.yaml
kubectl apply -f k8s/04-sensor-nodes.yaml
kubectl apply -f k8s/05-load-balancer.yaml
kubectl apply -f k8s/06-hpa.yaml

echo "⏳ Waiting for database and services to reach Ready state..."
kubectl rollout status deployment/postgres -n ds-sensor-network --timeout=90s || true
kubectl rollout status deployment/central-server -n ds-sensor-network --timeout=120s || true

# 4. Display Cluster Resources
echo "=========================================================="
echo "✅ KUBERNETES DEPLOYMENT COMPLETED!"
echo "=========================================================="
echo ""
echo "📋 Active Pods in namespace 'ds-sensor-network':"
kubectl get pods -n ds-sensor-network -o wide

echo ""
echo "⚖️  Services & Load Balancer:"
kubectl get svc -n ds-sensor-network

echo ""
echo "📈 Horizontal Pod Autoscaler (HPA):"
kubectl get hpa -n ds-sensor-network || true

echo "=========================================================="
echo "🌐 Accessing the Application:"
echo "👉 If running on Minikube:"
echo "   minikube service ds-load-balancer -n ds-sensor-network"
echo "👉 If running on Cloud / AWS EKS:"
echo "   Access via the External-IP shown in 'kubectl get svc ds-load-balancer -n ds-sensor-network'"
echo "👉 Port-forwarding shortcut:"
echo "   kubectl port-forward svc/ds-load-balancer 8000:8000 -n ds-sensor-network"
echo "=========================================================="
