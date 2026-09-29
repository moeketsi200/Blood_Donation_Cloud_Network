# 🩸 Blood Integration Broker — Systems Integration Assessment

## 1. Overview

This project is a Systems Integration (SI) Broker built with Java and Spring Boot. It acts as a central middleware to connect a legacy hospital system with modern external blood banks.

The primary task is to implement the core integration logic, demonstrating key Enterprise Integration Patterns (EIPs).

| Principle | Where you'll apply it |
| :--- | :--- |
| **Data Transformation** | Translating legacy XML formats into internal canonical JSON/Java objects. |
| **Protocol Translation** | Bridging SFTP polling with outbound REST and SOAP calls. |
| **Routing Logic** | Implementing scatter-gather patterns to query multiple blood banks and aggregate the results. |
| **Event Triggers** | Automatically notifying donors via an SMS service if blood banks report a shortage. |

---

## 2. Getting Started & How to Run

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose (Optional for containerized run)

---

### Option A: Run Locally via Maven (Recommended for Development)

1. **Compile & Build Project:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run Application Server:**
   ```bash
   mvn spring-boot:run
   ```
   *Alternatively, run the packaged fat JAR directly:*
   ```bash
   java -jar target/blood-integration-broker-1.0-SNAPSHOT.jar
   ```

3. **Access Frontend Dashboard:**
   Open your browser and navigate to:
   ```text
   http://localhost:8080/
   ```

---

### Option B: Run via Docker Compose (Containerized Production Mode)

1. **Build & Start Containers:**
   ```bash
   docker-compose up --build -d
   ```

2. **Check Container Status:**
   ```bash
   docker-compose ps
   ```

3. **Stream Application Logs:**
   ```bash
   docker-compose logs -f blood-broker
   ```

4. **Stop Container Services:**
   ```bash
   docker-compose down
   ```

---

### Troubleshooting Common Issues

#### 1. `Port 8080 was already in use`
- **Cause**: An instance of the Spring Boot application (or Docker container) is already running in the background listening on port `8080`.
- **Solution**:
  - Open `http://localhost:8080/` in your browser directly to access the running server.
  - Or terminate the active process using port 8080 before re-launching:
    ```bash
    fuser -k 8080/tcp
    # Or on Linux:
    kill $(lsof -t -i:8080)
    ```

#### 2. `GH013: Repository rule violations (Push cannot contain secrets)`
- **Cause**: GitHub Secret Scanning blocked `git push` because hardcoded AWS credentials were detected.
- **Solution**:
  - If the keys are test/dummy credentials, follow the GitHub URLs printed in the terminal output to unblock the push.
  - Ensure sensitive credentials are environment variables, and add configuration files to `.gitignore`.

---

## 3. System Architecture

### Integration Pipeline (EIP)
The system follows a clear integration flow from data ingestion to outbound communication.
```text
SFTP Poller (Inbound)
└── XmlToCanonicalTransformer
    └── PayloadEnricher
        └── BloodBankRouter (Scatter-Gather)
            ├── LegacyBankSoapAdapter (Bank A)
            ├── ModernBankRestAdapter (Bank B)
            └── (Aggregation Step)
                └── TwilioNotificationAdapter (Conditional: on shortage)
```

## Project Structure
```text
blood-integration-broker/
  pom.xml
  docker-compose.yml
  src/
    main/java/com/wethinkcode/bloodbroker/
      BrokerApplication.java
      config/
        SftpInboundConfig.java
        WebServiceConfig.java
      domain/
        HospitalRequest.java
        BloodInventoryStatus.java
        EmergencyAlert.java
      transformer/
        XmlToCanonicalTransformer.java
        PayloadEnricher.java
      router/
        BloodBankRouter.java
      adapter/
        LegacyBankSoapAdapter.java
        ModernBankRestAdapter.java
        TwilioNotificationAdapter.java
    test/java/com/wethinkcode/bloodbroker/
      transformer/
        XmlToCanonicalTransformerTest.java
        PayloadEnricherTest.java
      router/
        BloodBankRouterTest.java
```

**Do NOT modify any test files or BrokerApplication.java. All your work goes in `transformer`, `router`, and `adapter`.**

## Getting Started
Run the mock external systems first:
```bash
docker-compose up -d
```

Compile your project:
```bash
mvn compile
```

Run the test suite at any point to check your progress:
```bash
mvn test
```

## Implementation Steps
Work through the steps in order — each component builds the pipeline from inbound to outbound.

### Step 1 — Implement Canonical Data Model (Domain)
**Folder:** `src/main/java/com/wethinkcode/bloodbroker/domain/`
These classes represent your internal data format.

**HospitalRequest**
- Fields: `bloodType` (String), `unitsRequired` (int), `hospitalId` (String), `timestamp` (String)
- Methods: Getters and setters for all fields.

**BloodInventoryStatus**
- Fields: `bankName` (String), `unitsAvailable` (int), `emergencyShortage` (boolean)
- Methods: Getters and setters, and a method `isEmergencyShortage()` that returns true if `unitsAvailable == 0`.

### Step 2 — Implement Data Translation (Transformer)
**Folder:** `src/main/java/com/wethinkcode/bloodbroker/transformer/`
Convert the legacy XML into your canonical domain model and enrich it.

**XmlToCanonicalTransformer**
- Method: `public HospitalRequest transform(String legacyXml)`
- Details: Parses the incoming XML string (e.g., `<hospitalRequest><bloodType>O-Negative...</bloodType>...</hospitalRequest>`) into a `HospitalRequest` object.

**PayloadEnricher**
- Method: `public HospitalRequest enrich(HospitalRequest request)`
- Details: Adds the current system timestamp to the `HospitalRequest` before it gets routed.

### Step 3 — Implement Outbound Adapters (Adapter)
**Folder:** `src/main/java/com/wethinkcode/bloodbroker/adapter/`
Connect to the external mock systems.

**LegacyBankSoapAdapter**
- Method: `public BloodInventoryStatus checkStock(HospitalRequest request)`
- Details: Mocks a SOAP call to Blood Bank A. Returns a `BloodInventoryStatus` object.

**ModernBankRestAdapter**
- Method: `public BloodInventoryStatus queryInventory(HospitalRequest request)`
- Details: Mocks a REST call to Blood Bank B. Returns a `BloodInventoryStatus` object.

**TwilioNotificationAdapter**
- Method: `public void triggerDonorAlert(String bloodType)`
- Details: Logs an alert or sends an SMS simulation using Twilio SDK if stock is at zero.

### Step 4 — Implement EIP Routing (Router)
**Folder:** `src/main/java/com/wethinkcode/bloodbroker/router/`
Coordinate the adapters using a Scatter-Gather pattern.

**BloodBankRouter**
- Method: `public BloodInventoryStatus aggregate(List<BloodInventoryStatus> replies)`
- Details: Takes the responses from both Bank A and Bank B. Sums up the `unitsAvailable`. If the total units across all replies is `0`, it sets `emergencyShortage` to `true` on the aggregated result, otherwise `false`. Returns the aggregated `BloodInventoryStatus`.

---

## 5. Frontend Command Console & Web Dashboard

The project includes an interactive, web-based dashboard and visualizer built into the Spring Boot application at `src/main/resources/static/index.html`.

### How to Access
Start the Spring Boot application and navigate to:
```text
http://localhost:8080/
```

### Dashboard Capabilities & Features

1. **Emergency Hospital Dispatcher (XML Generator)**
   - Select requesting hospitals (e.g. Chris Hani Baragwanath, Groote Schuur, Steve Biko).
   - Select required blood type (`O-Negative`, `O-Positive`, `A-Positive`, etc.).
   - Interactive unit slider (`0 - 50` units).
   - Generates legacy XML payload (`<hospitalRequest>`) dynamically.

2. **Real-Time Integration Flow Visualizer**
   - Step-by-step state animations tracking the 5 key pipeline stages:
     1. **Inbound SQS Queue** — Message ingestion
     2. **Data Translation** — XML to Canonical transformation
     3. **Payload Enricher** — UTC timestamping
     4. **Scatter-Gather Router** — Concurrent SOAP & REST querying
     5. **Event Trigger (Amazon SNS)** — Conditional SMS/Email alerts on shortage

3. **Execution Trace & Infrastructure Telemetry**
   - **Pipeline Trace**: Displays raw JSON execution traces returned by the backend.
   - **System Telemetry**: Displays live status for AWS Fargate, SQS Queue, active adapters, and server region.

4. **AWS SQS Push & Emergency Shortage Simulation**
   - **Run Live Pipeline**: Executes the EIP pipeline synchronously for real-time demonstration.
   - **Push to SQS Queue**: Directly dispatches XML payloads to the configured AWS SQS queue URL (`POST /api/broker/sqs/publish`).
   - **Simulate 0 Units**: Instantly triggers an emergency 0-unit request to verify automated SNS notification dispatches.

5. **Connected Blood Banks Stock Gauge**
   - Visual card indicators displaying available units across Legacy Bank A (SOAP) and Modern Bank B (REST).

### Backend Controller Endpoints

- `POST /api/broker/process-xml` — Runs the EIP pipeline synchronously and returns detailed trace JSON.
- `POST /api/broker/sqs/publish` — Publishes legacy XML messages to AWS SQS queue.
- `GET /api/broker/telemetry` — Returns system health and AWS environment telemetry.

WTC-8UXPT3GE
