@echo off
cd /d "%~dp0"
TITLE Distributed Environmental Sensor Network (5-Process Architecture)
echo ===============================================================================
echo   DISTRIBUTED ENVIRONMENTAL SENSOR NETWORK (5-PROCESS ARCHITECTURE)
echo   1 Central Coordinator + 4 Sensor Nodes across 2 Dynamic Clusters
echo ===============================================================================
echo.

REM Check if jars exist, otherwise build
if not exist "central-server\target\central-server-1.0.0.jar" (
    echo [INFO] Building all modules with Maven...
    call mvnw.cmd clean package -DskipTests
)

echo [1/5] Launching Central Server (Spring Boot on Port 8000)...
start "Central Server (:8000)" cmd /k "java -jar central-server\target\central-server-1.0.0.jar"

echo Waiting 5 seconds for Central Server to boot...
timeout /t 5 /nobreak > nul

echo [2/5] Launching Cluster 1 - Sensor Node S1 (:8101)...
start "Sensor Node S1 (:8101) - Cluster C1" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor1.properties"

echo [3/5] Launching Cluster 1 - Sensor Node S2 (:8102 - Initial CH)...
start "Sensor Node S2 (:8102) - Cluster C1 CH" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor2.properties"

echo [4/5] Launching Cluster 2 - Sensor Node S3 (:8103)...
start "Sensor Node S3 (:8103) - Cluster C2" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor3.properties"

echo [5/5] Launching Cluster 2 - Sensor Node S4 (:8104 - Initial CH)...
start "Sensor Node S4 (:8104) - Cluster C2 CH" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor4.properties"

echo.
echo ===============================================================================
echo   ALL 5 PROCESSES STARTED SUCCESSFULLY!
echo ===============================================================================
echo   - Central Coordinator: http://localhost:8000
echo   - Cluster 1 Node S1:   http://localhost:8101
echo   - Cluster 1 Node S2:   http://localhost:8102 (Cluster Head)
echo   - Cluster 2 Node S3:   http://localhost:8103
echo   - Cluster 2 Node S4:   http://localhost:8104 (Cluster Head)
echo ===============================================================================
echo Opening browser consoles...
timeout /t 4 /nobreak > nul

start http://localhost:8000
start http://localhost:8101
start http://localhost:8102
start http://localhost:8103
start http://localhost:8104

pause
