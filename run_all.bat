@echo off
cd /d "%~dp0"
TITLE Distributed Environmental Sensor Network Launcher
echo ===============================================================
echo   DISTRIBUTED ENVIRONMENTAL SENSOR NETWORK (DS MINI PROJECT)
echo   Java Spring Boot Aggregator + Distributed Multi-Node Network
echo ===============================================================
echo.

if not exist "aggregator\target\aggregator-1.0.0.jar" (
    echo [INFO] First-time setup: Aggregator JAR not found. Building project...
    cd aggregator && call mvnw.cmd package -DskipTests && cd ..
)

echo [1/2] Starting Central Spring Boot Aggregator (REST :8080, gRPC :9090)...
start "DS Central Aggregator" cmd /k "java -jar aggregator\target\aggregator-1.0.0.jar"

echo Waiting 6 seconds for Spring Boot Aggregator & gRPC to initialize...
timeout /t 6 /nobreak > nul

echo [2/2] Starting Distributed Sensor Network (12 Nodes across 3 Clusters)...
start "DS Sensor Network Simulation" cmd /k "cd distributed-network && npm start"

echo.
echo ===============================================================
echo   ALL SERVICES STARTED SUCCESSFULLY!
echo.
echo   - React Live Dashboard:  http://localhost:8080/
echo   - REST Endpoints:        http://localhost:8080/api/clusters
echo   - H2 Database Console:   http://localhost:8080/h2-console
echo   - gRPC Service:          localhost:9090
echo ===============================================================
echo Opening dashboard in default browser...
start http://localhost:8080/

pause
