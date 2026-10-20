@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
TITLE Distributed Environmental Sensor Network (5-Process Architecture)
echo ===============================================================================
echo   DISTRIBUTED ENVIRONMENTAL SENSOR NETWORK (5-PROCESS ARCHITECTURE)
echo   1 Central Coordinator (:8099) + 4 Sensor Nodes across 2 Dynamic Clusters
echo ===============================================================================
echo.

REM Check if jars exist, otherwise build
if not exist "central-server\target\central-server-1.0.0.jar" (
    echo [INFO] Building all modules with Maven...
    call mvn clean package -DskipTests
)
if not exist "sensor-server\target\sensor-server-1.0.0.jar" (
    echo [INFO] Building all modules with Maven...
    call mvn clean package -DskipTests
)

echo Checking listening status for ports...
echo.

REM [1/5] Central Server (:8099)
netstat -ano | findstr /C:":8099 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo [INFO] Central Server is already running on port 8099. Skipping launch.
) else (
    echo [1/5] Launching Central Server on Port 8099...
    start "Central Server - Port 8099" cmd /k "java -jar central-server\target\central-server-1.0.0.jar --server.port=8099"
    echo Waiting 5 seconds for Central Server to boot...
    ping -n 6 127.0.0.1 >nul
)

REM [2/5] Sensor S1 (:8101)
netstat -ano | findstr /C:":8101 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo [INFO] Sensor Node S1 is already running on port 8101. Skipping launch.
) else (
    echo [2/5] Launching Cluster 1 - Sensor Node S1 on Port 8101...
    start "Sensor Node S1 - Port 8101" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor1.properties"
)

REM [3/5] Sensor S2 (:8102)
netstat -ano | findstr /C:":8102 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo [INFO] Sensor Node S2 is already running on port 8102. Skipping launch.
) else (
    echo [3/5] Launching Cluster 1 - Sensor Node S2 on Port 8102...
    start "Sensor Node S2 - Port 8102" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor2.properties"
)

REM [4/5] Sensor S3 (:8103)
netstat -ano | findstr /C:":8103 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo [INFO] Sensor Node S3 is already running on port 8103. Skipping launch.
) else (
    echo [4/5] Launching Cluster 2 - Sensor Node S3 on Port 8103...
    start "Sensor Node S3 - Port 8103" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor3.properties"
)

REM [5/5] Sensor S4 (:8104)
netstat -ano | findstr /C:":8104 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo [INFO] Sensor Node S4 is already running on port 8104. Skipping launch.
) else (
    echo [5/5] Launching Cluster 2 - Sensor Node S4 on Port 8104...
    start "Sensor Node S4 - Port 8104" cmd /k "java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor4.properties"
)

echo.
echo ===============================================================================
echo   ALL REQUIRED PROCESSES ARE RUNNING!
echo ===============================================================================
echo   - Central Coordinator: http://localhost:8099
echo   - Cluster 1 Node S1:   http://localhost:8101
echo   - Cluster 1 Node S2:   http://localhost:8102 (Cluster Head)
echo   - Cluster 2 Node S3:   http://localhost:8103
echo   - Cluster 2 Node S4:   http://localhost:8104 (Cluster Head)
echo ===============================================================================
echo Opening browser consoles...
ping -n 4 127.0.0.1 >nul

start http://localhost:8099
start http://localhost:8101
start http://localhost:8102
start http://localhost:8103
start http://localhost:8104

pause
