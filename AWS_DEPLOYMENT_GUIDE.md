
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
| **Distributed Databases Trade-offs** | **SQLite on Docker (WAL mode) vs S3 Object Store** | Embedded file-based ACID relational storage with WAL concurrency vs scalable distributed object store for telemetry archives. |

---

## 2. Architecture Diagram (AWS Cloud)

```
                            [ Internet / Faculty Browser ]
                                          │
                                   HTTP Port 80 (NGINX)
                                          ▼
                ┌──────────────────────────────────────────────────┐
                │          AWS EC2 Instance (t3.small / Ubuntu)    │
                │                                                  │
                │  ┌────────────────────────────────────────────┐  │
                │  │    NGINX LOAD BALANCER & REVERSE PROXY     │  │
                │  │    - Public Entry Point (Port 80)          │  │
                │  │    - Routes / to Central Server (:8099)    │  │
                │  │    - Routes /sensor1/ .. /sensor4/         │  │
                │  └──────┬───────────────────────────────┬─────┘  │
                │         │                               │        │
                │         ▼                               ▼        │
                │  ┌──────────────┐                ┌──────────────┐│
                │  │CENTRAL SERVER│                │ SENSOR NODES ││
                │  │(:8099)       │                │ S1-S4        ││
                │  │- SQLite DB   │                │ (:8101-8104) ││
                │  │  (/data/fw.db│                │ - Cluster C1 ││
                │  │- Coordinator │                │ - Cluster C2 ││
                │  └──────────────┘                └──────────────┘│
                └────────┬────────────────────────────────┬────────┘
                         │                                │
            Aggregated Batches (JSON)               Emergency Alert
                         │                                │
                         ▼                                ▼
              ┌─────────────────────┐          ┌─────────────────────┐
              │   Amazon S3 Bucket  │          │     AWS Lambda      │
              │  (Object Storage)   │          │    (Serverless)     │
              │  s3://forestwatch/  │          │  Emergency Dispatch │
              └─────────────────────┘          └─────────────────────┘
```

---

## 3. Step-by-Step AWS EC2 Deployment

### Step 1: Launch an AWS EC2 Instance
1. Log into your [AWS Management Console](https://console.aws.amazon.com/).
2. Navigate to **EC2** $\to$ **Instances** $\to$ **Launch Instance**.
3. Configure the following parameters:
   - **Name:** `ForestWatch-Node`
   - **Application & OS Image:** **Ubuntu Server 24.04 LTS (HVM)**, SSD Volume Type (64-bit x86).
   - **Instance Type:** `t3.small` (2 vCPU, 2 GB RAM — Recommended) or `t2.micro` (with swap enabled).
   - **Key Pair:** Select an existing `.pem` key pair or create a new one (e.g. `forestwatch-key.pem`).

### Step 2: Configure the Security Group (Network Inbound Rules)
In the Network Settings section, create a Security Group with these **Inbound Rules**:

| Type | Protocol | Port Range | Source | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **SSH** | TCP | `22` | `<YOUR-ADMIN-IP>/32` | Secure Terminal Access (Restricted to Admin CIDR) |
| **HTTP** | TCP | `80` | `0.0.0.0/0` | Layer 7 NGINX Reverse Proxy & Load Balancer |
| **HTTPS** | TCP | `443` | `0.0.0.0/0` | Optional TLS Ingress |

*(Note: Sensor node ports `8101–8104`, database files, Actuator, Prometheus, and Grafana are NOT exposed to the public internet. They are routed securely through NGINX on port 80 or accessed via SSH tunnel).*

Click **Launch Instance**.

---

### Step 3: Connect and Run Deployment
Open PowerShell or local terminal where your `.pem` key is stored:

```bash
# 1. Connect to EC2
ssh -i "forestwatch-key.pem" ubuntu@<YOUR-EC2-PUBLIC-IP>

# 2. Clone the repository
git clone https://github.com/combox1234/Forest-watch.git forestwatch
cd forestwatch

# 3. Configure Production Environment Secrets
cp .env.example .env
# Edit .env with secure random keys:
nano .env

# 4. Launch Core Fleet with Docker Compose
docker compose up -d --build

# Optional: To launch the monitoring stack (Prometheus, Grafana, Loki):
# docker compose --profile monitoring up -d
```

The system will:
1. Build the multi-stage Maven images for `central-server` and `sensor-server`.
2. Mount persistent storage for SQLite at `/data` (`sqlite_data` volume).
3. Initialize the SQLite schema with WAL mode (`PRAGMA journal_mode=WAL;`).
4. Start Central Server, 4 Sensor Nodes, and NGINX Reverse Proxy.

---

### Step 4: SQLite Database Inspection and Backup
To inspect or create a consistent backup of the SQLite database while the application is running:

```bash
# Safely inspect database schema using sqlite3 CLI inside container:
docker exec -it fw-central-server sqlite3 /data/forestwatch.db ".tables"

# Safely take an online hot-backup using SQLite's online backup API:
docker exec -it fw-central-server sqlite3 /data/forestwatch.db ".backup /data/backup_$(date +%Y%m%d).db"
```

---

## 4. Connecting AWS S3 & AWS Lambda (Unit IV)

### Setting Up Amazon S3 (Distributed Object Storage)
1. Go to AWS Console $\to$ **Amazon S3** $\to$ **Create Bucket**.
2. **Bucket Name:** `forestwatch-telemetry-archive` (or unique name).
3. **Region:** `ap-south-1` (Asia Pacific - Mumbai).
4. In your `.env`, set:
   ```env
   AWS_S3_BUCKET=forestwatch-telemetry-archive
   AWS_REGION=ap-south-1
   ```

### Setting Up AWS Lambda (Serverless Architecture)
1. Go to AWS Console $\to$ **AWS Lambda** $\to$ **Create Function**.
2. **Function Name:** `EmergencyAlertDispatcher`
3. **Runtime:** **Python 3.12**
4. Paste the handler code located in: `aws/lambda/emergency_dispatcher.py`
5. Click **Deploy**.

---

## 5. Practical Faculty Demonstration Sequence

For a comprehensive evaluation or faculty viva, follow this exact step-by-step demonstration sequence covering application workflows, distributed algorithms, observability, and cloud infrastructure:

### Prerequisites & Startup Command
Ensure the core application and monitoring stack are running:
```bash
# On the EC2 host:
cd ~/forestwatch
# Start core application fleet + observability stack
docker compose --profile monitoring up -d
```

### Accessing Protected Interfaces
All administrative interfaces (Prometheus `:9090`, Grafana `:3000`, Alertmanager `:9093`, Loki `:3100`) are securely bound to `127.0.0.1` and never exposed directly to the public internet. Access them via a secure SSH local port-forwarding tunnel from your local machine:
```bash
# Run on your local laptop:
ssh -i forestwatch-key.pem -L 3000:localhost:3000 -L 9090:localhost:9090 -L 9093:localhost:9093 ubuntu@<EC2-PUBLIC-IP>
```

---

### Step-by-Step Demonstration Workflow

#### 1. Public Web Entry Point & Role-Based Access Control
- Open `http://<EC2-PUBLIC-IP>/` in your browser.
- Point out that this is routed via the **NGINX Layer 7 Reverse Proxy & Load Balancer (Port 80)** on AWS EC2 in `ap-south-1` (Mumbai).
- Sign in with an operator account (`parth.admin` or `arjun.operator`) to showcase Spring Security 6 form authentication and BCrypt password verification.
- Show that internal SQLite database storage is active at `/data/forestwatch.db` operating in high-concurrency **Write-Ahead Logging (WAL)** mode.

#### 2. Open Grafana via Secure Administrative Access
- Navigate to `http://localhost:3000` via the active SSH tunnel.
- Sign in as `admin` using the secure `GRAFANA_ADMIN_PASSWORD` provisioned via your `.env` file (or Kubernetes Secret).
- Point out that Grafana runs non-root, public sign-ups are disabled, and no monitoring ports are opened in the AWS Security Group.

#### 3. ForestWatch Dashboard & Live Fleet Metrics
- In Grafana, open the provisioned dashboard: **"ForestWatch Operations & Fleet Overview"** (`uid: forestwatch-fleet-ops`).
- Highlight the real-time panel telemetry:
  - **Fleet Health Row:** Status indicators for Central Server (`:8099`) and all 4 sensor nodes (S1–S4).
  - **HTTP Request Throughput Panel:** Live request rates calculated from Spring Boot Actuator Micrometer metrics.
  - **JVM Heap Memory Utilization Panel:** Real-time JVM memory tracking across running JVM instances.

#### 4. Prometheus Target Health Inspection
- Navigate to `http://localhost:9090/targets` via the SSH tunnel.
- Show that Prometheus has 5 active, healthy scrape targets:
  - `central-server:8099` (`/actuator/prometheus`, role: coordinator)
  - `sensor-1:8101` (`/actuator/prometheus`, cluster: C1, node: S1)
  - `sensor-2:8102` (`/actuator/prometheus`, cluster: C1, node: S2)
  - `sensor-3:8103` (`/actuator/prometheus`, cluster: C2, node: S3)
  - `sensor-4:8104` (`/actuator/prometheus`, cluster: C2, node: S4)
- Emphasize that metrics are scraped at 5-second intervals directly over the private Docker bridge network (`forestwatch-net`).

#### 5. Alert Rules & Simulated Incident
- In Prometheus, click **Alerts** (`http://localhost:9090/alerts`) or open Alertmanager at `http://localhost:9093`.
- Show the 4 defined operational rules from `monitoring/alert_rules.yml`:
  1. `CentralServerDown` (`up{job="forestwatch-central"} == 0` for 15s)
  2. `SensorNodeDown` (`up{job="forestwatch-sensor-nodes"} == 0` for 15s)
  3. `HighHttpErrorRate` (`rate(http_server_requests_seconds_count{status=~"5.."}[1m]) > 0.05`)
  4. `HighJvmHeapUsage` (`jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85`)
- *(Optional Test)*: Stop one sensor container (`docker stop fw-sensor-3`) and watch Prometheus transition the alert from `Pending` $\to$ `Firing`, subsequently routing the webhook to Alertmanager. Restart it (`docker start fw-sensor-3`) to show automatic resolution.

#### 6. Centralized Log Collection via Grafana Alloy & Loki Exploration
Promtail reached end-of-life on March 2, 2026 and has been replaced with Grafana Alloy (`grafana/alloy:v1.4.3`), which collects logs and routes them to Loki (`grafana/loki:3.1.1`).

##### A. Running a Sample Log through Alloy into Loki
To verify the log ingestion pipeline end-to-end on the host:
1. Ensure the monitoring stack is running:
   ```bash
   docker compose --profile monitoring up -d loki alloy
   ```
2. Write a structured sample log line into the application log directory:
   ```bash
   sudo mkdir -p /var/log/forestwatch
   echo "$(date -u +'%Y-%m-%dT%H:%M:%SZ') [INFO] CentralCoordinator - ForestWatch sample telemetry ingestion verification via Grafana Alloy" | sudo tee -a /var/log/forestwatch/app.log
   ```
3. Verify Alloy state and metrics (listening on `http://127.0.0.1:12345`):
   ```bash
   curl -s http://127.0.0.1:12345/-/ready
   ```
4. Verify Loki received the log stream:
   ```bash
   curl -G -s "http://127.0.0.1:3100/loki/api/v1/query" --data-urlencode 'query={job="forestwatch-logs"}'
   ```

##### B. Querying in Grafana Explore
- In Grafana (`http://127.0.0.1:3000`), navigate to **Explore** $	o$ select the **Loki** datasource.
- Run a LogQL query to inspect real-time logs ingested by Grafana Alloy from the host and application containers:
  ```logql
  {job="forestwatch-logs"} |= "CentralCoordinator"
  ```
- Alternatively, query sensor telemetry logs:
  ```logql
  {job="forestwatch-logs"} |= "Heartbeat"
  ```
- Demonstrate how Grafana Alloy decouples log exploration from individual container filesystems without requiring privileged Docker socket access.

#### 7. Architecture Overview: How DevOps Components Fit Together
Provide the faculty with the architectural synthesis:
- **Terraform:** Declares reproducible AWS cloud infrastructure in Mumbai (`ap-south-1`): VPC, public subnet, route tables, security group with restricted SSH admin CIDR, and t3.small EC2 instance.
- **Ansible:** Configures the EC2 host idempotently: installs Docker & Docker Compose, sets up 2GB swap space for low-memory instance stability, clones the repository, deploys `.env` secrets, and validates Compose configs.
- **Docker & Docker Compose:** Isolates 6 application containers (Central Coordinator, 4 Sensor Nodes, NGINX Load Balancer) and 5 observability containers (Prometheus v2.54.1, Grafana 11.2.0, Alertmanager v0.27.0, Loki 3.1.1, Grafana Alloy v1.4.3) with single-command profile management.
- **Kubernetes:** Manifests provide production container orchestration with single-writer SQLite PVC persistence (`ReadWriteOnce`), non-root security context (`runAsUser: 1000`, `fsGroup: 1000`), HPA autoscaling for stateless sensors, and internal ClusterIP services for observability.
- **CI/CD (GitHub Actions):** Validates every pull request and push: runs 20+ automated Maven unit & integration tests (verifying SQLite WAL & busy timeout), validates Docker compose configurations, and lints Kubernetes manifests.

---

## 6. Continuous Delivery (CD) to AWS EC2 via Systems Manager & GitHub Actions

### 6.1 Distinction Between CI and CD
* **Continuous Integration (CI — `.github/workflows/ci.yml`):** Runs on every push and pull request to `main`. It automatically compiles Java 21 multi-module Maven source, runs 21 unit and integration tests (including SQLite WAL mode and busy timeout verification, and Spring Security RBAC), validates Docker Compose configurations with placeholder credentials, builds Docker images, checks Terraform formatting, and lints Kubernetes manifests using `kubeconform`. CI never touches AWS infrastructure or deploys code.
* **Continuous Delivery (CD — `.github/workflows/cd.yml`):** Runs **only** when CI has successfully passed for a commit on the `main` branch (or via manual maintainer dispatch). It deploys the verified commit to the live AWS EC2 host using AWS Systems Manager Run Command, authenticated via OpenID Connect (OIDC).

### 6.2 Production Approval Gate & GitHub Settings Configuration
Deployment is governed by GitHub's `production` environment:
* **CRITICAL: Manual Setup in GitHub Repository Settings:**
  Declaring `environment: name: production` in `.github/workflows/cd.yml` attaches the deployment job to the environment, but **GitHub will only enforce human review if configured in Repository Settings**:
  1. Navigate to **Repository Settings** $\to$ **Environments**.
  2. Click **New environment** and enter name: `production`.
  3. Under **Deployment protection rules**, check **Required reviewers**.
  4. Select designated project maintainers/evaluators as required signers.
  5. Under **Deployment branches**, optionally restrict deployments to `Selected branches` (specifying `main`).
* **Approval Workflow:** When a commit on `main` passes CI, the CD workflow transitions to `Waiting for review`. A human maintainer must explicitly click **Review deployments** $\to$ **Approve and deploy** before any AWS credentials can be assumed or SSM commands dispatched.
* **Feature Branch & Pull Request Protection:** Pull requests and feature branch commits can never trigger deployment.

### 6.3 AWS Authentication via GitHub Actions OIDC (Zero Hardcoded Keys)
Rather than storing static AWS access keys in GitHub Secrets, the CD pipeline uses cryptographic OpenID Connect (OIDC) identity federation:
1. **GitHub Identity Provider in AWS IAM:** Ensure AWS IAM has OpenID Connect provider `https://token.actions.githubusercontent.com` with client ID `sts.amazonaws.com`.
2. **IAM Deployment Role Trust Policy:**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Principal": {
           "Federated": "arn:aws:iam::<AWS-ACCOUNT-ID>:oidc-provider/token.actions.githubusercontent.com"
         },
         "Action": "sts:AssumeRoleWithWebIdentity",
         "Condition": {
           "StringEquals": {
             "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
           },
           "StringLike": {
             "token.actions.githubusercontent.com:sub": "repo:combox1234/Forest-watch:environment:production"
           }
         }
       }
     ]
   }
   ```
3. **Least-Privilege Role Permissions:** Attach a policy granting `ssm:SendCommand` and `ssm:GetCommandInvocation` restricted to the target EC2 instance ARN.

### 6.4 EC2 Instance Preparation for Systems Manager (SSM)
The target EC2 instance must be reachable by Systems Manager:
* **IAM Instance Profile:** Attach an IAM role containing AWS managed policy `AmazonSSMManagedInstanceCore` to the EC2 instance.
* **SSM Agent:** On Ubuntu 24.04, the SSM agent is active by default. Verify on the instance with:
  ```bash
  sudo systemctl status snap.amazon-ssm-agent.amazon-ssm-agent.service
  ```
* **Network Egress:** The EC2 instance must be able to reach AWS SSM endpoints (via Internet Gateway on public subnet or VPC endpoints).
* **Repository Location:** The repository must be cloned at `/home/ubuntu/forestwatch`.

### 6.5 GitHub Repository Variables Setup
In **Settings** $\to$ **Secrets and variables** $\to$ **Actions** $\to$ **Variables**, configure:
| Variable Name | Description | Example Value |
| :--- | :--- | :--- |
| `AWS_REGION` | AWS Deployment Region | `ap-south-1` |
| `AWS_DEPLOYMENT_ROLE_ARN` | IAM Role assumed via OIDC | `arn:aws:iam::123456789012:role/ForestWatchGithubDeployerRole` |
| `EC2_INSTANCE_ID` | Target EC2 Instance ID | `i-0123456789abcdef0` |
| `EC2_PUBLIC_IP` | (Optional) Public IP for environment link | `13.233.x.x` |

### 6.6 Safe Deployment Execution & SQLite Data Protection
The remote execution runs `deploy_aws.sh`, which enforces strict data safety:
1. **Clean Working Tree Check:** Verifies no uncommitted tracked files exist on the host before updating.
2. **Safe Git Checkout:** Fetches `origin/main` and checks out the specific commit SHA validated by CI. Never executes `git reset --hard` or `git clean -fd`.
3. **Pre-Deployment Hot Backup:** Executes SQLite online hot backup `sqlite3 /data/forestwatch.db ".backup /data/forestwatch_backup_TIMESTAMP.db"` while the container is running.
4. **Volume Preservation:** Uses `docker compose up -d` to recreate application containers. Never runs `docker compose down -v` or `docker volume rm`.
5. **Health Check Gate:** Polls `http://localhost:8099/actuator/health` up to 60 seconds. If unhealthy, logs are printed to GitHub Actions output and the workflow exits with code 1.
6. **Isolated Secrets:** Host secrets in `/home/ubuntu/forestwatch/.env` are never printed in logs or sent across CI/CD networks.

### 6.7 Managing Optional Monitoring Services on Low-Memory VM
Because the AWS EC2 `t3.small` instance operates with ~2 GiB RAM, the core deployment script starts only the 6 essential application containers.
To start or stop the monitoring stack without affecting core application telemetry:
```bash
# Start monitoring containers on EC2:
docker compose --profile monitoring up -d

# Stop monitoring containers when conserving memory:
docker compose --profile monitoring stop prometheus grafana alertmanager loki alloy
```

### 6.8 Diagnosing a Failed Deployment
If a CD deployment fails in GitHub Actions:
1. Inspect the **Await Execution & Verify Fleet Health** step output in the GitHub Actions run log. The full SSM Run Command output and recent Spring Boot container logs are streamed directly to the runner.
2. To inspect directly on the EC2 host via terminal:
   ```bash
   ssh -i forestwatch-key.pem ubuntu@<EC2-PUBLIC-IP>
   cd /home/ubuntu/forestwatch
   docker compose ps
   docker compose logs --tail=100 central-server
   ```
3. If an emergency rollback of the container image is needed, run:
   ```bash
   # Checkout previous known-good commit:
   git checkout <PREVIOUS_COMMIT_SHA>
   docker compose up -d --build
   ```
   *(Note: The SQLite database file `/data/forestwatch.db` remains persistent across image rollbacks).*
