# Distributed Environmental Sensor Network (5-Process Spring Boot Architecture)

**Course:** Distributed Systems (Final Year B.Tech — Curriculum Regulations)  
**Project Title:** Distributed Environmental Sensor Network with Edge Aggregation, Leader Election, Synchronization, and Fault-Tolerant Coordination  

A multi-module Maven Spring Boot distributed systems implementation featuring **ONE Central Server** and **FOUR independent Sensor Server processes** organized into **TWO logical clusters**, communicating entirely over real HTTP/REST network protocols.

---

## 🏛️ System Architecture

```
                                  CENTRAL SERVER
                                Spring Boot (:8000)
                           [Central Storage & Dashboard]
                                        │
                    ┌───────────────────┴───────────────────┐
                    │                                       │
                CLUSTER 1                               CLUSTER 2
           (Alpha Forest Canopy)                   (Beta River Basin)
                    │                                       │
            ┌───────┴───────┐                       ┌───────┴───────┐
            │               │                       │               │
        Sensor 1        Sensor 2                Sensor 3        Sensor 4
      Spring Boot     Spring Boot             Spring Boot     Spring Boot
        (:8101)         (:8102)                 (:8103)         (:8104)
      ROLE: SENSOR   ROLE: CLUSTER HEAD       ROLE: SENSOR   ROLE: CLUSTER HEAD
```

### Key Architectural Principles
1. **5 Real Independent Operating System Processes:**
   - **Central Server:** `http://localhost:8000` (JPA/H2/PostgreSQL, Global State, Dashboard)
   - **Sensor Node 1:** `http://localhost:8101` (Cluster C1 Member)
   - **Sensor Node 2:** `http://localhost:8102` (Cluster C1 Initial Cluster Head)
   - **Sensor Node 3:** `http://localhost:8103` (Cluster C2 Member)
   - **Sensor Node 4:** `http://localhost:8104` (Cluster C2 Initial Cluster Head)
2. **Dynamic Cluster Head Role (No Fixed CH Servers):**
   - Cluster Head is a **dynamic role**, NOT a dedicated physical server or special port.
   - Sensor servers share a single reusable Spring Boot codebase (`sensor-server`) parameterized by property profiles.
   - Within each cluster, nodes elect a coordinator using the **Bully Election Algorithm**.
   - If S2 (CH on `:8102`) crashes, S1 (`:8101`) detects the failure via heartbeat timeout, runs an election, promotes itself to `CLUSTER_HEAD`, and begins running edge aggregation on port 8101 without any port change!
3. **Real Inter-Process Communication:**
   - All inter-process communication occurs via real HTTP REST calls (`RestTemplate`).
   - Standard message envelopes contain `senderId`, `receiverId`, `clusterId`, `messageType`, `timestamp`, `vectorClock`, and `payload`.
4. **Zero Blockchain / Distributed Ledger Overhead:**
   - Strictly focused on foundational distributed systems concepts: Bully Election, Vector Clocks, Heartbeat Failure Detectors, Mutual Exclusion, Edge Computing, and Centralized Storage.

---

## 📂 Project Directory Structure

```
distributed-sensor-network/ (workspace root)
├── pom.xml                               # Parent multi-module Maven POM
├── shared/                               # Common library jar
│   ├── pom.xml
│   └── src/main/java/com/ds/sensor/shared/
│       ├── message/
│       │   ├── Message.java              # Normalized inter-node message envelope
│       │   ├── MessageType.java          # HEARTBEAT, ELECTION, SENSOR_DATA, etc.
│       │   ├── VectorClockData.java      # 4-component vector clock [S1,S2,S3,S4]
│       │   ├── SensorReadingData.java    # Temperature, humidity, motion, battery
│       │   ├── AggregatedData.java       # AVG, MIN, MAX, sample count
│       │   └── AlertData.java            # Shared alert payload with deduplication
│       └── model/
│           ├── NodeRole.java             # SENSOR vs CLUSTER_HEAD
│           └── NodeStatus.java           # ACTIVE, FAILED, SUSPECTED
│
├── central-server/                       # Central Server Spring Boot App (:8000)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ds/sensor/central/
│       │   │   ├── CentralServerApplication.java
│       │   │   ├── controller/
│       │   │   │   ├── CentralViewController.java  # Serves central dashboard at /
│       │   │   │   └── CentralApiController.java   # /api/system-status, /api/aggregate, /api/logs
│       │   │   ├── service/
│       │   │   │   ├── CentralService.java         # Node registration, heartbeat tracking
│       │   │   │   ├── GlobalStateService.java     # Causal frontier & cluster state
│       │   │   │   └── AggregationStorageService.java
│       │   │   ├── model/                      # JPA Entities (8 tables)
│       │   │   └── repository/                 # Spring Data JPA Repositories
│       │   └── resources/
│       │       ├── application.properties      # server.port=8000, H2 & Postgres configs
│       │       └── templates/
│       │           └── dashboard.html          # Interactive Central Dashboard
│
├── sensor-server/                        # Reusable Sensor Server Spring Boot App
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ds/sensor/node/
│       │   │   ├── SensorServerApplication.java
│       │   │   ├── config/
│       │   │   │   └── NodeConfig.java         # node.id, node.port, node.cluster, node.peers
│       │   │   ├── controller/
│       │   │   │   ├── SensorViewController.java   # Serves sensor UI at /
│       │   │   │   └── SensorApiController.java    # /api/message, /api/generate-reading, etc.
│       │   │   ├── service/
│       │   │   │   ├── SensorService.java          # State, telemetry, periodic simulation
│       │   │   │   ├── CommunicationService.java   # Real HTTP dispatch to peers & Central
│       │   │   │   ├── VectorClockService.java     # [S1,S2,S3,S4] causality tracking
│       │   │   │   ├── ElectionService.java        # Bully algorithm implementation
│       │   │   │   ├── HeartbeatService.java       # Periodic liveness checking & timeout
│       │   │   │   ├── MutualExclusionService.java # Alert deduplication coordinator
│       │   │   │   └── EdgeAggregationService.java # Sliding-window AVG/MIN/MAX computing
│       │   └── resources/
│       │       ├── application.properties      # Defaults
│       │       └── templates/
│       │           └── sensor.html             # Rich Interactive Sensor Node UI
│       └── test/
│           └── java/com/ds/sensor/node/        # Unit tests (Bully, VectorClock, Aggregation, Mutex)
│
├── sensor-config/                        # Configuration profiles for 4 independent nodes
│   ├── sensor1.properties                # S1 (:8101), C1, peer=http://localhost:8102
│   ├── sensor2.properties                # S2 (:8102), C1, peer=http://localhost:8101 (Initial CH)
│   ├── sensor3.properties                # S3 (:8103), C2, peer=http://localhost:8104
│   └── sensor4.properties                # S4 (:8104), C2, peer=http://localhost:8103 (Initial CH)
│
├── docker-compose.yml                    # Multi-container orchestration (5 servers + postgres)
├── run_all.bat                           # 1-Click launcher opening 5 distinct CMD windows + browser tabs
└── README.md                             # Technical guide & viva preparation
```

---

## 🚀 Quick Start (Windows)

### Option 1: 1-Click Batch Launcher (`run_all.bat`)
Run from PowerShell or Command Prompt:
```cmd
.\run_all.bat
```
This launcher:
1. Verifies that all modules are built (`mvn clean package -DskipTests`).
2. Starts the **Central Server** in a titled command window on port `8000`.
3. Waits 5 seconds for the database and coordinator to initialize.
4. Spawns 4 separate titled command windows for Sensor 1 (`:8101`), Sensor 2 (`:8102`), Sensor 3 (`:8103`), and Sensor 4 (`:8104`).
5. Opens all 5 web consoles in your default browser:
   - Central Dashboard: `http://localhost:8000`
   - Sensor 1 Console: `http://localhost:8101`
   - Sensor 2 Console: `http://localhost:8102`
   - Sensor 3 Console: `http://localhost:8103`
   - Sensor 4 Console: `http://localhost:8104`

---

### Option 2: Step-by-Step Manual Launch

#### 1. Compile and Package All Modules
```cmd
.\mvnw.cmd clean package -DskipTests
```

#### 2. Terminal 1: Launch Central Server
```cmd
java -jar central-server\target\central-server-1.0.0.jar
```

#### 3. Terminal 2: Launch Sensor 1 (Cluster 1, Port 8101)
```cmd
java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor1.properties
```

#### 4. Terminal 3: Launch Sensor 2 (Cluster 1 Leader, Port 8102)
```cmd
java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor2.properties
```

#### 5. Terminal 4: Launch Sensor 3 (Cluster 2, Port 8103)
```cmd
java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor3.properties
```

#### 6. Terminal 5: Launch Sensor 4 (Cluster 2 Leader, Port 8104)
```cmd
java -jar sensor-server\target\sensor-server-1.0.0.jar --spring.config.additional-location=sensor-config/sensor4.properties
```

---

## 🧪 Automated Unit Test Suite

Run all unit tests across the multi-module project:
```cmd
.\mvnw.cmd test
```

| Test Class | Core Distributed Systems Mechanism Validated |
|---|---|
| `VectorClockTest` | 4-component vector clocks $[S_1, S_2, S_3, S_4]$, local increment, element-wise max merge, happens-before causal ordering ($A \to B$), and concurrency detection ($A \parallel B$). |
| `BullyElectionTest` | Priority ordering ($S_4 > S_3 > S_2 > S_1$), failover when leader is unreachable, and automatic self-promotion to `CLUSTER_HEAD`. |
| `EdgeAggregationTest` | Accurate mathematical reduction (AVG, MIN, MAX, sample count, active sensor count) over sliding-window sample buffers. |
| `MutualExclusionTest` | Alert deduplication: First alert request granted critical section execution; concurrent duplicate alert within time window is suppressed/deduplicated. |

---

## 🎯 Viva Demonstration Scenarios

### Scenario 1: Multi-Process Boot & Topology Inspection
1. Run `run_all.bat`.
2. Inspect the 5 console windows and the Central Dashboard at `http://localhost:8000`.
3. Verify the initial state:
   - **Cluster 1 (Alpha Forest Canopy):** $S_1$ is `SENSOR` (:8101), $S_2$ is `CLUSTER_HEAD` (:8102).
   - **Cluster 2 (Beta River Basin):** $S_3$ is `SENSOR` (:8103), $S_4$ is `CLUSTER_HEAD` (:8104).
   - Central Server displays `Active Sensors: 4/4` and `Active Cluster Heads: 2`.

### Scenario 2: Edge Aggregation & Upstream Bandwidth Reduction
1. Open $S_1$ (:8101) and click **[ Generate Reading ]** several times.
2. In the console and Network Log, observe $S_1$ sending raw `SENSOR_DATA` to its Cluster Head $S_2$ (:8102).
3. On $S_2$ (:8102), click **[ Edge Aggregate & Send ]** (or wait for the periodic aggregation).
4. Check the Central Server Dashboard (:8000) under **Edge Aggregated Reports**:
   - Central Server receives **1 compact summary** containing `Avg Temperature`, `Min/Max Temp`, `Avg Humidity`, and `Sample Count`.
   - Upstream network bandwidth is reduced by ~75%!

### Scenario 3: Vector Clock Causality & Concurrency Tracking
1. Note the current Vector Clock on $S_1$ (:8101) and $S_2$ (:8102).
2. Click **[ Generate Reading ]** on $S_1$. Notice index 0 increments: $[1, 0, 0, 0]$.
3. Click **[ Generate Reading ]** on $S_2$. Notice index 1 increments: $[0, 1, 0, 0]$.
4. In the Central Dashboard Communication Monitor, observe that events with clocks $[1, 0, 0, 0]$ and $[0, 1, 0, 0]$ are mathematically evaluated as **CONCURRENT** ($S_1 \parallel S_2$).
5. When $S_1$ sends a message to $S_2$, $S_2$ merges clocks and ticks: $[1, 2, 0, 0]$, establishing a strict **Happens-Before** causal relationship.

### Scenario 4: Mutual Exclusion & Alert Deduplication
1. On $S_1$ (:8101), click **[ Animal Detected (Mutex) ]**.
2. $S_1$ dispatches an `ALERT_REQUEST` to Cluster Head $S_2$.
3. $S_2$ grants permission (`ALERT_GRANTED`) and forwards the alert to Central Server.
4. Immediately (within 10 seconds), click **[ Animal Detected (Mutex) ]** on $S_2$ (:8102).
5. $S_2$ detects an active alert in the same cluster, rejects the duplicate request (`ALERT_REJECTED`), and deduplicates it.
6. Central Server records the incident once with `sourceNodes: ["S1", "S2"]`.

### Scenario 5: Cluster Head Failure & Bully Election Failover
1. On Cluster Head $S_2$ (`http://localhost:8102`), click **[ Simulate Failure (Crash) ]**.
2. $S_2$ enters `FAILED` status and halts heartbeat pings.
3. Within 5 seconds, $S_1$ (`http://localhost:8101`) detects missed heartbeats from $S_2$.
4. $S_1$ initiates a Bully Election: sends `ELECTION` to $S_2$. Since $S_2$ is down and cannot reply with `ANSWER`, $S_1$ declares itself coordinator!
5. $S_1$ updates its role: `ROLE: CLUSTER_HEAD` while remaining on port `8101`!
6. $S_1$ notifies Central Server: Central Dashboard immediately updates Cluster 1 Head to $S_1$ (:8101).
7. $S_1$ takes over running edge aggregation!

### Scenario 6: Bully Recovery & Preemption
1. On $S_2$ (`http://localhost:8102`), click **[ Recover Node (Bully Takeover) ]**.
2. $S_2$ comes back online and inspects the cluster state: it notices its Bully Priority (2) is higher than current leader $S_1$ (1).
3. $S_2$ bullies $S_1$, demotes $S_1$ back to `SENSOR`, and reclaims `CLUSTER_HEAD` on port `8102`.
4. Central Dashboard updates Cluster 1 Head back to $S_2$.

---

## 💡 Key Distributed Systems Concepts Implemented

1. **Bully Leader Election Algorithm:**
   - Scoped to cluster subsets ($C_1 = \{S_1, S_2\}$, $C_2 = \{S_3, S_4\}$).
   - Nodes with higher process identifiers bully lower nodes upon failure detection or recovery.
2. **Lamport Vector Clocks:**
   - 4-component integer vector $[S_1, S_2, S_3, S_4]$ attached to every inter-node message envelope.
   - Provides strict partial ordering and concurrency detection across distributed nodes without physical clock synchronization.
3. **Heartbeat Failure Detector:**
   - Active push liveness pings exchanged every 2 seconds.
   - Adaptive timeout window (5s threshold) transitioning node status from `ACTIVE` to `SUSPECTED` and `FAILED`.
4. **Mutual Exclusion via Coordinator Arbitration:**
   - Centralized coordinator within each cluster arbitrates access to critical alert dispatch actions, preventing duplicate concurrent alerts from overwhelming the upstream network.
5. **Edge Aggregation:**
   - In-network statistical computation (mean, min, max, event counts) performed at the edge layer, minimizing cloud/central bandwidth utilization.
6. **Distributed Object Storage (AWS S3):**
   - Cold historical telemetry batches archived into AWS S3 (`s3://ds-environmental-telemetry-archive/`) with content-addressed object keys and MD5 ETags (Unit IV).
7. **Serverless Architectures (AWS Lambda):**
   - Event-driven invocation of AWS Lambda serverless function (`EmergencyAlertDispatcher`) on mutual exclusion alert triggers with sub-100ms execution and automatic scaling (Unit IV).

---

## ☁️ AWS Cloud Deployment (EC2 & Docker)

### Option 3: 1-Click AWS EC2 Deployment
Deploy directly to a free-tier AWS EC2 instance (Ubuntu 22.04 / 24.04 LTS):

1. **Launch an AWS EC2 Instance:**
   - AMI: **Ubuntu Server 24.04 LTS**
   - Instance Type: **t2.micro** or **t3.small** (Free Tier eligible)
   - Security Group Rules:
     - Port 22 (SSH)
     - Port 8000 (Central Server & Global Dashboard)
     - Ports 8101–8104 (Sensor Node Web Consoles)

2. **Connect & Deploy:**
   ```bash
   ssh -i your-key.pem ubuntu@<ec2-public-ip>
   git clone <your-repo-url> ds-sensor-network
   cd ds-sensor-network
   chmod +x deploy_aws.sh
   ./deploy_aws.sh
   ```

3. **Open Dashboard:**
   - Central Coordinator & Global State Monitor: `http://<ec2-public-ip>:8000`
   - Node Consoles: `http://<ec2-public-ip>:8101` to `:8104`

---

## ☸️ Kubernetes & Load Balancer Deployment (Unit IV)

### Option 4: Deploying on Kubernetes with Layer 7 Load Balancing
Deploy to Minikube, Docker Desktop Kubernetes, Kind, or AWS EKS:

1. **Deploy all resources with 1 command:**
   * **Windows:**
     ```cmd
     .\deploy_k8s.bat
     ```
   * **Linux / macOS / Cloud:**
     ```bash
     chmod +x deploy_k8s.sh
     ./deploy_k8s.sh
     ```
   * **Direct `kubectl`:**
     ```bash
     kubectl apply -f k8s/
     ```

2. **Access via Load Balancer:**
   * Kubernetes Service `ds-load-balancer` (Layer 4/Layer 7 Load Balancer) distributes incoming traffic across multiple `central-server` pod replicas using **Round-Robin**.
   * Port-forward shortcut:
     ```bash
     kubectl port-forward svc/ds-load-balancer 8000:8000 -n ds-sensor-network
     ```
   * Dashboard URL: `http://localhost:8000`


