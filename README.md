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

## 2. Getting Started

### Prerequisites
- Java 21+
- Maven
- Docker

### Setup

1.  **Run Mock Services:**
    The project requires mock external systems (SFTP, SOAP, REST) to be running. Start them using Docker Compose.
    ```bash
    docker-compose up -d
    ```

2.  **Compile the Project:**
    ```bash
    mvn compile
    ```

3.  **Run Tests:**
    You can run the test suite at any point to check your implementation against the requirements.
    ```bash
    mvn test
    ```

> **Note:** Do NOT modify any test files or `BrokerApplication.java`. All your work should be within the `transformer`, `router`, and `adapter` packages.

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
