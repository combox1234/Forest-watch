@echo off
REM ==============================================================================
REM Kubernetes & Load Balancer Automated Deployment Launcher (Windows)
REM Course: Distributed Systems (Unit IV: Case Studies: Kubernetes & Load Balancing)
REM Project: Distributed Environmental Sensor Network
REM ==============================================================================

echo ==========================================================
echo [KUBERNETES] Deploying Distributed Sensor Network...
echo ==========================================================

where kubectl >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'kubectl' CLI not found.
    echo Please enable Kubernetes in Docker Desktop or start Minikube / Kind.
    pause
    exit /b 1
)

echo.
echo [1/4] Applying Namespace and Secrets...
kubectl apply -f k8s\00-namespace.yaml
kubectl apply -f k8s\01-config.yaml

echo.
echo [2/4] Deploying PostgreSQL Database...
kubectl apply -f k8s\02-postgres.yaml

echo.
echo [3/4] Deploying Central Server Replicas (2 Pods) ^& Sensor Nodes...
kubectl apply -f k8s\03-central-server.yaml
kubectl apply -f k8s\04-sensor-nodes.yaml

echo.
echo [4/5] Deploying Layer 7 Load Balancer ^& Autoscaler (HPA)...
kubectl apply -f k8s\05-load-balancer.yaml
kubectl apply -f k8s\06-hpa.yaml

echo.
echo [5/5] Deploying Prometheus ^& Grafana Observability...
kubectl apply -f k8s\07-monitoring.yaml

echo.
echo ==========================================================
echo [SUCCESS] Kubernetes Resources Successfully Deployed!
echo ==========================================================
echo.
echo Active Pods:
kubectl get pods -n ds-sensor-network -o wide

echo.
echo Active Services & Load Balancer:
kubectl get svc -n ds-sensor-network

echo.
echo ==========================================================
echo How to access:
echo If using Docker Desktop / Minikube:
echo   Run: kubectl port-forward svc/ds-load-balancer 8099:8099 -n ds-sensor-network
echo Then open in browser:
echo   http://localhost:8099
echo   Grafana: kubectl port-forward svc/grafana-svc 3000:3000 -n ds-sensor-network -> http://localhost:3000
echo ==========================================================
pause
