# Distributed Systems Mini Project — Viva Preparation & Syllabus Mapping Guide

**Course:** Distributed Systems (Final Year B.Tech — Curriculum Regulations 2023 w.e.f. 2026–27)  
**Project Title:** Distributed Environmental Sensor Network with Edge Aggregation, Leader Election, Synchronization, and Fault-Tolerant Coordination  

---

## 1. Quick Viva Elevator Pitch (30 Seconds)

> *"Our project simulates a software-defined distributed environmental sensor network where 12 heterogeneous nodes organize into 3 autonomous clusters. Each cluster dynamically elects an edge coordinator using the **Bully Leader Election Algorithm (Garcia-Molina, 1982)**. When a coordinator crashes, surviving nodes detect missed heartbeats and autonomously re-elect a successor within seconds.*  
> *Rather than flooding raw telemetry, edge coordinators compute windowed **Edge Aggregations** to cut upstream bandwidth by ~80%, submitting compact summaries to a central **Spring Boot Aggregator** over **gRPC (HTTP/2 Protocol Buffers)**.*  
> *Distributed events carry **Vector Clocks (Lamport causality)** to reconstruct causal order even under network delays, while a **Token-based Mutual Exclusion protocol** deduplicates simultaneous alert actions across multiple sensors detecting the same emergency."*

---

## 2. Syllabus Mapping (Unit-by-Unit)

### Unit I: Distributed-System Architectures, Middleware & RPC
- **Concept:** Multi-tier architectural styles, middleware abstractions, RPC mechanisms, failure models.
- **Our Implementation:**
  - **4-Tier Architecture:** Sensor Layer (producers) $\to$ Edge Layer (elected coordinators) $\to$ Central Aggregator Layer (Spring Boot + gRPC Netty) $\to$ Presentation Layer (Real-time React WebSocket dashboard).
  - **Middleware & RPC:** Cluster heads use **gRPC** (`service AggregationService` with Protocol Buffers) over HTTP/2 to communicate with the Central Aggregator.
  - **Failure Model:** Crash-stop and crash-recovery failure models with heartbeat liveness detection.

### Unit II: Communication Paradigms
- **Concept:** Message-oriented communication, peer-to-peer messaging, stream-oriented communication.
- **Our Implementation:**
  - **Asynchronous Inter-node Messaging:** Sensor nodes exchange asynchronous socket messages (`HEARTBEAT`, `ELECTION`, `OK`, `COORDINATOR`, `DATA`).
  - **Stream-Oriented Communication:** Central Aggregator pushes real-time telemetry, leader shifts, and alert updates to the React frontend over persistent **WebSockets** (`/ws`).

### Unit III: Synchronization, Election & Global State
- **Concept:** Logical clocks vs physical clocks, Lamport timestamps, Vector clocks, Election algorithms, Mutual exclusion.
- **Our Implementation:**
  - **Vector Clocks (Algorithm 10.2):** Each node $i$ increments $V[i]$ on local events, attaches $V$ to messages, and merges incoming vectors with $V[k] = \max(V[k], M[k])$. The aggregator evaluates $V_A < V_B$ (happens-before) vs $V_A \parallel V_B$ (concurrency).
  - **Bully Leader Election (Algorithm 10.1):** When a coordinator misses 3 heartbeats (timeout $\Delta t$), surviving nodes initiate the Bully election sending `ELECTION` to higher-priority nodes. If no `OK` arrives within timeout, the highest active node crowns itself coordinator and announces to the cluster.
  - **Mutual Exclusion (Algorithm 10.4):** Prevents duplicate execution of shared emergency alerts (e.g. wildfire or intrusion alarms) when multiple sensors concurrently detect the same physical event.

### Unit IV: Emerging Distributed Paradigms & Storage
- **Concept:** Distributed object-based systems, serverless architectures, cloud case studies (AWS), containerization, distributed databases.
- **Our Implementation:**
  - **Distributed Object Storage (AWS S3):** Cluster head edge aggregates are periodically batched and archived into Amazon S3 (`s3://ds-environmental-telemetry-archive/`) as immutable, content-addressed JSON objects with MD5 ETags.
  - **Serverless Architecture (AWS Lambda):** Critical emergency alerts (`ANIMAL_DETECTED`, `WILDFIRE_ALERT`) invoke a serverless AWS Lambda handler (`EmergencyAlertDispatcher`) that executes within 35–80ms to broadcast notifications via Amazon SNS.
  - **Cloud Case Study & Virtualization (AWS EC2 & Docker):** Deployed on AWS EC2 using multi-container Docker Compose orchestration, fulfilling the syllabus requirements for virtualization, containerization, and AWS cloud deployment.
  - **Edge Aggregation (Algorithm 10.5):** Coordinators aggregate raw sensor data over sliding windows (avg temp, min/max temp, humidity, sample count) before forwarding upstream, reducing message volume by 75–85%.
  - **Data Persistence:** Operational data persisted in PostgreSQL (`sensor`, `cluster`, `reading`, `alert`, `s3_archive_log`, `lambda_invocation_log`) with embedded H2 fallback for zero-setup local runs.

---

## 3. High-Frequency Viva Questions & Model Answers

### Q1: Why use Vector Clocks instead of simple physical timestamps (NTP/Wall clock)?
**Answer:** Physical clocks on distributed machines suffer from clock skew and drift; NTP synchronization is non-instantaneous and cannot guarantee causal ordering across independent nodes. Vector clocks track **causal dependency** ($A \to B$) purely through message exchanges without relying on synchronized physical clocks.

### Q2: How does the Bully algorithm handle concurrent election triggers?
**Answer:** If multiple nodes simultaneously detect a leader failure, each node sends `ELECTION` messages to nodes with higher IDs. Higher ID nodes reply `OK` to lower ID nodes, effectively "bullying" them out of contention. The highest surviving ID node receives no higher response, declares itself leader, and broadcasts `COORDINATOR` to all members.

### Q3: What happens when the failed former leader comes back online?
**Answer:** When the recovered node boots up, it compares its priority ID with the current coordinator. Because it has higher priority, it initiates an election, receives no opposition, and bullies the existing coordinator to reclaim leadership (demonstrating graceful preemptive recovery).

### Q4: Why is gRPC preferred for Edge-to-Aggregator communication over standard HTTP REST?
**Answer:** gRPC operates over HTTP/2 multiplexing, supports bi-directional streaming, and serializes payloads into compact binary Protocol Buffers. This yields significantly lower serialization latency, smaller payload size, and lower CPU overhead than textual JSON REST.

### Q5: Why did the project intentionally exclude Blockchain?
**Answer:** As stated in the design document (Section 1 and Appendix A), environmental sensor telemetry requires high-throughput, low-latency edge coordination and causal synchronization. Blockchain consensus (PoW/PoS) introduces proof verification delays, transaction fees, and unnecessary complexity that does not align with the core distributed systems syllabus objectives.

### Q6: How does our project demonstrate Unit IV Serverless Architectures on AWS?
**Answer:** When an edge cluster triggers a mutual-exclusion emergency alert, it initiates an event-driven call to an AWS Lambda function (`EmergencyAlertDispatcher`). This function executes in a serverless microVM runtime on-demand without provisioning persistent server capacity, automatically scaling to burst alert volumes and broadcasting notifications with low latency.

### Q7: Why use AWS S3 Object Storage alongside PostgreSQL relational storage?
**Answer:** PostgreSQL provides ACID transactional guarantees and indexing for real-time node state and topology queries. In contrast, AWS S3 represents a **Distributed Object-Based Storage System** offering 99.999999999% (11 9's) durability and high-throughput write scalability for long-term historical telemetry dumps at a fraction of relational database storage cost.

### Q8: How does our project implement Kubernetes container orchestration (Unit IV)?
**Answer:** The project defines declarative Kubernetes manifests (`k8s/00` to `k8s/06`) deploying the system across an isolated namespace (`ds-sensor-network`). It runs multiple replicas of the Central Server coordinator backed by `livenessProbe` and `readinessProbe` health checks, a PersistentVolumeClaim for PostgreSQL, and Horizontal Pod Autoscaling (HPA) to scale dynamically between 2 and 5 pods.

### Q9: How does the Load Balancer distribute traffic across backend replicas?
**Answer:** Incoming client requests hit a Layer 7 Load Balancer / Kubernetes Service (`ds-load-balancer`) configured with a **Round-Robin** routing algorithm. The load balancer alternates traffic between healthy pod replicas (`Pod Alpha` and `Pod Beta`). If a pod crashes or fails its readiness probe, the load balancer automatically withdraws it from the endpoint pool to prevent service disruption.


