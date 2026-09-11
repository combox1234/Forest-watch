# Distributed Environmental Sensor Network

**Course:** Distributed Systems (Final Year B.Tech — Curriculum Regulations 2023 w.e.f. 2026–27)  
**Mini Project Design Document Implementation**  

A software-simulated distributed environmental sensor network with edge aggregation, leader election, synchronization, and fault-tolerant coordination.

---

## Architecture Overview

The system implements a 4-layer distributed architecture:

1. **Sensor Layer**: 12 heterogeneous nodes across 3 clusters (`cluster-1`, `cluster-2`, `cluster-3`) generating temperature, humidity, water quality, and motion readings. Each node maintains a local **Vector Clock** (Algorithm 10.2).
2. **Edge Layer**: Dynamically elected **Cluster Heads** running the **Bully Leader Election Algorithm** (Algorithm 10.1) and **Heartbeat Failure Detection** (Algorithm 10.3). Heads perform sliding-window **Edge Aggregation** (Algorithm 10.5) and **Mutual Exclusion** (Algorithm 10.4) for shared alert actions.
3. **Central Aggregator Layer**: **Java Spring Boot 3** server with:
   - **gRPC Server** (`AggregationService` on port `9090`)
   - **REST API** (`/api/*` on port `8080`)
   - **WebSocket Stream** (`/ws` on port `8080`)
   - **JPA Persistence** (Embedded H2 for instant local execution + PostgreSQL profile for production/Docker).
4. **Presentation Layer**: Ultra-modern **React + Vite** live telemetry dashboard embedded directly into the Spring Boot server or available via Vite dev server.

---

## Quick Start (Windows)

### Option 1: One-Click Runner (Recommended)
Simply double-click `run_all.bat` or run in terminal:
```cmd
.\run_all.bat
```
This will:
1. Start the Java Spring Boot Aggregator (`:8080`, gRPC `:9090`)
2. Start the 12-node distributed sensor network simulation
3. Automatically launch the live dashboard in your default browser at `http://localhost:8080/`

---

### Option 2: Step-by-Step Manual Run

#### Step 1: Start the Spring Boot Aggregator
```cmd
java -jar aggregator\target\aggregator-1.0.0.jar
```
*(Runs on `http://localhost:8080/`, gRPC on `localhost:9090`)*

#### Step 2: Start the Distributed Sensor Network
In a new terminal:
```cmd
cd distributed-network
npm start
```
*(Registers 3 clusters, starts 12 nodes, emits heartbeats, sends edge summaries via gRPC)*

#### Step 3: Open the Dashboard
Open your web browser and visit:
👉 **`http://localhost:8080/`**

---

## Docker Compose Deployment

To run with full multi-container orchestration and PostgreSQL:
```bash
docker compose up --build
```
This spins up:
- `ds-postgres`: PostgreSQL 16 database on port `5432`
- `ds-aggregator`: Java Spring Boot backend on ports `8080` & `9090`
- `ds-sensor-network`: Multi-cluster simulated sensor nodes

---

## Algorithms Implemented

| Algorithm | Design Doc | Description |
|-----------|------------|-------------|
| **Bully Leader Election** | Section 10.1 | Detects coordinator failure via heartbeats, exchanges `ELECTION` and `OK` messages, crowns highest-priority node coordinator. |
| **Vector Clocks** | Section 10.2 | Maintains logical time vector $V$, updates with component-wise maximum, reconstructs causal ordering under delay. |
| **Heartbeat Failure Detection** | Section 10.3 | Periodic liveness pings; transitions node state: `HEALTHY` $\to$ `SUSPECTED` $\to$ `FAILED`. |
| **Mutual Exclusion** | Section 10.4 | Single shared execution of critical alerts when multiple sensors detect the same event within a window. |
| **Edge Aggregation** | Section 10.5 | Computes compact statistical summaries at edge heads, reducing upstream gRPC traffic by ~80%. |

---

## Test Scenarios & Verification (Section 16)

| Test ID | Scenario | How to Demonstrate | Expected Result |
|---------|----------|-------------------|-----------------|
| **T01** | Normal Operation | Open dashboard at `http://localhost:8080/` | Continuous edge summaries, stable leaders, 12 healthy nodes. |
| **T02** | Leader Failure | Click **"Kill Alpha Leader"** in Fault Console | S4 fails $\to$ S2/S3 run Bully election $\to$ S3 elected new leader in ~1.5s. |
| **T03** | Simultaneous Events | Click **"Trigger Concurrent Fire Alert"** | S1 & S2 detect alert $\to$ Mutex grants lock to S1 $\to$ S2 duplicate suppressed. |
| **T04** | Causal Ordering | Click **"Inject Network Latency"** | S1 delayed $\to$ Vector clock stamps ensure correct causal reconstruction. |
| **T05** | Node Recovery | Click **"Recover Node"** on a failed sensor | Node rejoins cluster $\to$ marks HEALTHY $\to$ starts emitting heartbeats. |
| **T07** | Load & Edge Savings | View **"Edge Aggregation"** section | Demonstrates 75–85% upstream network bandwidth reduction. |

---

## REST Endpoints

- `GET /api/sensors` — List all 12 sensors and health status
- `GET /api/clusters` — List 3 clusters and current coordinator IDs
- `GET /api/readings` — Recent aggregated summaries
- `GET /api/alerts` — Recent environmental alerts and mutual exclusion state
- `GET /api/elections` — Historical Bully election logs
- `POST /api/simulation/fault/{nodeId}` — Inject crash on a specific node
- `POST /api/simulation/recover/{nodeId}` — Recover a node
- `POST /api/simulation/trigger-alert` — Trigger concurrent alert condition
- `POST /api/simulation/recover-all` — Recover all nodes to healthy
- `H2 Console`: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:sensordb`, User: `sa`, Password: empty)
