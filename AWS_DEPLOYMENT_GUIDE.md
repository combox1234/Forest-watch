# AWS Cloud Deployment & Syllabus Integration Guide
**Course:** Distributed Systems (Final Year B.Tech — Curriculum Regulations 2023 w.e.f. 2026–27)  
**Project:** Distributed Environmental Sensor Network with Edge Aggregation, Leader Election, Synchronization, and Fault-Tolerant Coordination

---

## 1. Unit IV Syllabus Mapping Overview

| Syllabus Topic (Unit IV) | Implemented AWS Architecture Component | Role in the Distributed Network |
| :--- | :--- | :--- |
| **Case Studies: AWS** | **Amazon EC2 (Elastic Compute Cloud)** | Multi-container host running 5 isolated OS processes across Docker bridge network. |
| **Virtualization & Containerization** | **Docker & Docker Compose** | OS-level virtualization isolating Central Server and 4 Sensor Nodes with independent ports and IP addressing. |
| **Distributed Object-Based Systems** | **Amazon S3 (Simple Storage Service)** | Cold storage tier for immutable, content-addressed aggregated telemetry objects with MD5 ETags. |
| **Serverless Architectures** | **AWS Lambda (`EmergencyAlertDispatcher`)** | Event-driven serverless function invoked upon mutual-exclusion emergency alerts with sub-100ms execution latency. |
| **Distributed Databases Trade-offs** | **PostgreSQL on Docker (ACID) vs S3 Object Store** | Relational OLTP database for topology and state vs scalable distributed object store for telemetry logs. |

---

## 2. Architecture Diagram (AWS Cloud)

```
                            [ Internet / Examiner Browser ]
                                          │
                         HTTP Port 8000   │   HTTP Ports 8101-8104
                                          ▼
                ┌──────────────────────────────────────────────────┐
                │          AWS EC2 Instance (t2.micro / Ubuntu)    │
                │                                                  │
                │  ┌────────────────────────────────────────────┐  │
                │  │    CENTRAL SERVER (Spring Boot :8000)      │  │
                │  │    - Global Causal Frontier Monitor        │  │
                │  │    - Dynamic Topology & Leader Registry    │  │
                │  │    - Real-time Interactive Dashboard       │  │
                │  └──────┬───────────────────────────────┬─────┘  │
                │         │ HTTP                          │ HTTP   │
                │         ▼                               ▼        │
                │  ┌──────────────┐                ┌──────────────┐│
                │  │  CLUSTER 1   │                │  CLUSTER 2   ││
                │  │  S1 (:8101)  │                │  S3 (:8103)  ││
                │  │  S2 (:8102)  │                │  S4 (:8104)  ││
                │  └──────────────┘                └──────────────┘│
                └────────┬────────────────────────────────┬────────┘
                         │                                │
            Aggregated Batches (JSON)               Emergency Alert
                         │                                │
                         ▼                                ▼
              ┌─────────────────────┐          ┌─────────────────────┐
              │   Amazon S3 Bucket  │          │     AWS Lambda      │
              │  (Object Storage)   │          │    (Serverless)     │
              │  s3://ds-telemetry/ │          │  Emergency Dispatch │
              └─────────────────────┘          └─────────────────────┘
```

---

## 3. Step-by-Step AWS EC2 Deployment (Free Tier Eligible)

### Step 1: Launch an AWS EC2 Instance
1. Log into your [AWS Management Console](https://console.aws.amazon.com/).
2. Navigate to **EC2** $\to$ **Instances** $\to$ **Launch Instance**.
3. Configure the following parameters:
   - **Name:** `Distributed-Systems-Sensor-Network`
   - **Application & OS Image:** **Ubuntu Server 24.04 LTS (HVM)**, SSD Volume Type (64-bit x86).
   - **Instance Type:** `t2.micro` (1 vCPU, 1 GB RAM — Free Tier eligible) or `t3.small` (2 vCPU, 2 GB RAM).
   - **Key Pair:** Select an existing `.pem` key pair or create a new one (e.g. `ds-key.pem`).

### Step 2: Configure the Security Group (Network Inbound Rules)
In the Network Settings section, create a Security Group with these **Inbound Rules**:

| Type | Protocol | Port Range | Source | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **SSH** | TCP | `22` | `0.0.0.0/0` | Secure Terminal Access |
| **HTTP** | TCP | `80` | `0.0.0.0/0` | Layer 7 NGINX Load Balancer |
| **Custom TCP** | TCP | `8000` | `0.0.0.0/0` | Central Server Dashboard |
| **Custom TCP** | TCP | `8101` | `0.0.0.0/0` | Sensor Node 1 Web Console |
| **Custom TCP** | TCP | `8102` | `0.0.0.0/0` | Sensor Node 2 Web Console |
| **Custom TCP** | TCP | `8103` | `0.0.0.0/0` | Sensor Node 3 Web Console |
| **Custom TCP** | TCP | `8104` | `0.0.0.0/0` | Sensor Node 4 Web Console |

Click **Launch Instance**.

---

### Step 3: Connect and Run the 1-Click Deployment Script
Open PowerShell or your local terminal where your `.pem` key is stored:

```bash
# 1. Connect to EC2
ssh -i "ds-key.pem" ubuntu@<YOUR-EC2-PUBLIC-IP>

# 2. Clone the repository
git clone https://github.com/<your-username>/<your-repo-name>.git ds-network
cd ds-network

# 3. Execute the automated deployment script
chmod +x deploy_aws.sh
./deploy_aws.sh
```

The script will automatically:
1. Install Docker Engine and the Docker Compose plugin.
2. Build the multi-module Maven containers for `central-server` and `sensor-server`.
3. Launch the PostgreSQL database and all 5 Spring Boot processes in background detached mode.
4. Print your public dashboard URLs!

---

## 4. Connecting AWS S3 & AWS Lambda (Unit IV)

### Setting Up Amazon S3 (Distributed Object Storage)
1. Go to the AWS Console $\to$ **Amazon S3** $\to$ **Create Bucket**.
2. **Bucket Name:** `ds-environmental-telemetry-archive` (or your unique name).
3. **Region:** `us-east-1` (US East N. Virginia).
4. Keep **Block all public access** enabled.
5. In your EC2 `.env` or `docker-compose.yml`, set:
   ```env
   AWS_S3_BUCKET=ds-environmental-telemetry-archive
   AWS_REGION=us-east-1
   ```

### Setting Up AWS Lambda (Serverless Architecture)
1. Go to AWS Console $\to$ **AWS Lambda** $\to$ **Create Function**.
2. **Function Name:** `EmergencyAlertDispatcher`
3. **Runtime:** **Python 3.12**
4. Paste the handler code located in:
   `aws/lambda/emergency_dispatcher.py`
5. Click **Deploy**.

> [!NOTE]
> **Built-in Offline Emulation Mode:** If you present your project offline or without entering AWS billing credentials, the project contains an automated **in-memory AWS emulation engine**. It computes real MD5 ETags, generates official AWS Request IDs, and simulates cold vs warm execution latencies (35–80ms) directly in the UI!

---

## 5. Live Viva Demonstration Script (AWS & Unit IV)

When demonstrating to the professor or external examiner:

1. **Show Live EC2 Deployment:**
   - Open your browser to `http://<EC2-PUBLIC-IP>:8000`.
   - Point out that this is running on an active **AWS EC2 Ubuntu Virtual Machine**, demonstrating **Unit I (Virtualization)** and **Unit IV (AWS Cloud Case Study)**.

2. **Demonstrate Distributed Object Storage (AWS S3):**
   - Scroll to the **☁️ Unit IV: AWS Cloud Services** panel.
   - Click **[ 📤 Sync Archive to AWS S3 ]**.
   - Show the new record appearing with:
     - Canonical S3 Object Key (e.g. `telemetry/c1/20261013_...json`)
     - MD5 ETag checksum validating data integrity
     - Causal Vector Clock $[S_1, S_2, S_3, S_4]$ preserved with the archive.
   - Explain: *"This demonstrates Distributed Object Storage (Unit IV), storing cold immutable telemetry at scale rather than overloading the operational relational database."*

3. **Demonstrate Serverless Architecture (AWS Lambda):**
   - Click **[ ⚡ Invoke Lambda Alert ]** (or trigger an animal detection alert on Sensor 1 `:8101`).
   - Observe the immediate serverless invocation in the table:
     - Unique AWS Request ID
     - Execution Duration: $\sim 45\text{ ms}$
     - Status: `200 OK`
     - Action: `SNS_TOPIC_BROADCAST`
   - Explain: *"This demonstrates Serverless Architecture (Unit IV). Rather than running dedicated alert daemon threads, alert events trigger an event-driven AWS Lambda microVM that automatically scales to zero when idle."*
