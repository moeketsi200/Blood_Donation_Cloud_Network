# 🩸 Frontend Command Console & AWS Cloud Visualizer — Documentation

## 📌 Overview

The **Blood Donation Integration Broker Frontend** is a modern, single-page web dashboard integrated directly into the Spring Boot application ([index.html](file:///home/wtc09/Personal_projects/Blood_Donation_Cloud_Network/src/main/resources/static/index.html)).

It provides real-time visualization and interactive execution controls for the Enterprise Integration Pattern (EIP) pipeline, including legacy XML payload generation, AWS SQS message publishing, live pipeline execution tracing, system telemetry, and stock level monitoring.

---

## 🎨 Technology & Architecture

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Framework & Structure** | Vanilla HTML5 & JavaScript (ES6+) | Lightweight, dependency-free client application. |
| **Design System & Styling** | Custom Modern Vanilla CSS | Dark mode glassmorphic interface with CSS custom properties, backdrop filters, radial glows, and responsive CSS grid. |
| **Typography & Icons** | Google Fonts (`Outfit`, `Plus Jakarta Sans`, `Fira Code`) + FontAwesome 6 | Clean corporate cloud telemetry aesthetics with monospace code styling. |
| **Backend Integration** | Spring Boot REST Controller | Connects to endpoints exposed in `BrokerRestController.java`. |

---

## 🛠 Features & Capabilities

### 1. Emergency Hospital Dispatcher (Dynamic XML Generator)
- **Hospital Selection**: Select requesting hospitals (Chris Hani Baragwanath, Groote Schuur, Steve Biko, Charlotte Maxeke, or CLI Terminal Client).
- **Blood Type Selection**: Select target blood types (`O-Negative`, `O-Positive`, `A-Negative`, `A-Positive`, `B-Negative`, `B-Positive`, `AB-Negative`, `AB-Positive`).
- **Interactive Slider**: Adjust requested blood units between `0` and `50`.
- **Live XML Preview**: Dynamically formats and renders valid legacy XML requests:
  ```xml
  <hospitalRequest>
      <bloodType>O-Negative</bloodType>
      <unitsRequired>5</unitsRequired>
      <hospitalId>HOSP-CHRIS-HANI-BARAGWANATH</hospitalId>
  </hospitalRequest>
  ```

---

### 2. Interactive EIP Pipeline Visualizer
The dashboard features an animated 5-step EIP flow visualizer:

1. **Inbound SQS Queue** — Message ingestion (replacing legacy SFTP).
2. **Data Translation** — Parses legacy XML into canonical `HospitalRequest` Java object.
3. **Payload Enricher** — Stamps ISO-8601 UTC timestamp on the request.
4. **Scatter-Gather Router** — Concurrently queries Bank A (SOAP) & Bank B (REST).
5. **Event Trigger (Amazon SNS)** — Triggers SMS/Email emergency alerts if total stock == 0.

During execution, each pipeline node transitions through state styling (`Standby` → `Processing...` → `✅ OK` / `🚨 SHORTAGE ALERT SENT`).

---

### 3. Action Controls

- **⚡ Run Live Pipeline**: Calls `POST /api/broker/process-xml` to execute the full EIP pipeline synchronously and display step-by-step trace JSON.
- **☁️ Push to SQS Queue**: Calls `POST /api/broker/sqs/publish` to test asynchronous AWS SQS message ingestion.
- **🚨 Simulate 0 Units**: Automatically sets required units to `0` and triggers an emergency shortage scenario to test Amazon SNS notification firing.

---

### 4. Telemetry & Execution Trace Console
- **Pipeline Trace Tab**: Displays formatted JSON response containing full execution pipeline telemetry (`step1_raw_xml`, `step2_transformed_canonical`, `step3_enriched_canonical`, `step4_adapter_replies`, `step5_aggregated_status`, `emergency_alert_triggered`).
- **System Telemetry Tab**: Fetches live server environment status from `GET /api/broker/telemetry` (AWS Region, active SQS queue URL, connected adapters, server uptime).

---

### 5. Blood Bank Stock Level Gauges
- Displays inventory levels across connected adapters:
  - **Legacy Bank A (SOAP)**
  - **Modern Bank B (REST)**
- Highlighted emergency stock status (`0 Units` displayed with red warnings).

---

## 📡 API Endpoint Reference

| Endpoint | Method | Content-Type | Description |
| :--- | :--- | :--- | :--- |
| `/api/broker/process-xml` | `POST` | `application/xml` | Executes synchronous EIP pipeline and returns full execution trace JSON. |
| `/api/broker/sqs/publish` | `POST` | `application/xml` | Publishes XML payload directly to AWS SQS Queue. |
| `/api/broker/telemetry` | `GET` | `application/json` | Returns system telemetry and AWS environment status. |

---

## 🚀 How to Run & Access

### Step 1: Run the Backend & Frontend Application

#### Method 1 — Local Maven Execution:
```bash
# Compile and package
mvn clean package -DskipTests

# Start Spring Boot application server
mvn spring-boot:run
```

#### Method 2 — Docker Container Execution:
```bash
# Build and launch application container
docker-compose up --build -d

# View live application logs
docker-compose logs -f blood-broker
```

---

### Step 2: Open Frontend Command Console

Open your web browser and navigate to:
```text
http://localhost:8080/
```
*(Static dashboard automatically loads from `/src/main/resources/static/index.html`)*

---

### Step 3: Run Interactive Simulations

1. **Test Standard Dispatch**: Click **⚡ Run Live Pipeline** to run the synchronous EIP pipeline with the selected hospital and blood type.
2. **Test SQS Integration**: Click **☁️ Push to SQS Queue** to test asynchronous message ingestion.
3. **Test Emergency Shortage**: Click **🚨 Simulate 0 Units** to force a zero-stock scenario and observe the SNS alert dispatch notification.

---

## 🛠 Troubleshooting

### 1. `Port 8080 was already in use`
- **Why it occurs**: The Spring Boot server is already running in the background.
- **Solution**:
  - Simply open `http://localhost:8080/` directly in your browser.
  - Or stop the running background process via terminal:
    ```bash
    fuser -k 8080/tcp
    ```

### 2. `GH013: Repository rule violations (Push cannot contain secrets)`
- **Why it occurs**: GitHub Secret Protection blocked `git push` due to AWS credentials detected in repository commits.
- **Solution**:
  - Follow the unblock links provided in your `git push` terminal output if they are dummy/test keys.
  - Remove hardcoded secret keys from tracked files and store them as environment variables.

