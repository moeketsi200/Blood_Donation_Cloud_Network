# ☁️ Blood Integration Broker — AWS Cloud Migration (Step 2)

## 1. Overview

This step migrates the Blood Integration Broker from local/mock services into real AWS cloud infrastructure.

You will replace the two empty placeholder classes (`SftpInboundConfig` and `WebServiceConfig`) with real AWS integrations, and update the `TwilioNotificationAdapter` to publish emergency alerts to a real cloud notification service.

| Component to Replace | What It Becomes on AWS |
| :--- | :--- |
| **SFTP Poller (SftpInboundConfig)** | Amazon SQS queue — the broker polls this queue for incoming hospital XML messages instead of a local folder. |
| **Mock Twilio Adapter** | Amazon SNS topic — sends a real SMS or email to donors when an emergency shortage is detected. |
| **application.yml (empty)** | Populated with your AWS region, SQS queue URL, and SNS topic ARN. |
| **pom.xml** | Add the AWS SDK v2 dependency for SQS and SNS clients. |

---

## 2. Prerequisites

Before starting, make sure you have all of these in place:

- Java 21+
- Maven
- Docker (for local testing)
- AWS CLI installed and configured (`aws configure` — you will need your Access Key, Secret Key, and region)
- An active AWS account (free tier is sufficient)

> **Note:** The `aws/` folder in this project contains the AWS CLI binary. Run `./aws/dist/aws --version` to confirm it is working before you start.

---

## 3. AWS Infrastructure You Will Create

You need to manually create two AWS resources in your AWS Console (or CLI) **before** touching any Java code.

### Resource 1 — SQS Queue (Inbound)

This queue replaces the SFTP folder. The hospital system will put XML messages into this queue. The broker will poll it.

- **Queue Name:** `blood-broker-inbound`
- **Queue Type:** Standard Queue
- **What goes in it:** Raw XML strings in the same format the SFTP poller used to receive (e.g., `<hospitalRequest><bloodType>O-Negative</bloodType>...</hospitalRequest>`)
- **After creating it:** Copy the **Queue URL** — you will need it for `application.yml`.

### Resource 2 — SNS Topic (Outbound Alerts)

This topic replaces the mock Twilio log. When an emergency shortage is detected, the broker publishes to this topic, which then fans out to email or SMS subscribers.

- **Topic Name:** `blood-broker-emergency-alerts`
- **Topic Type:** Standard
- **Subscription:** Add at least one subscription (your email address is easiest to test with)
- **After creating it:** Copy the **Topic ARN** — you will need it for `application.yml`.

---

## 4. System Architecture (After This Step)

```text
AWS SQS Queue (Inbound)
└── SqsInboundConfig  ← you will implement this (replaces SftpInboundConfig)
    └── XmlToCanonicalTransformer  (unchanged)
        └── PayloadEnricher  (unchanged)
            └── BloodBankRouter  (unchanged)
                ├── LegacyBankSoapAdapter  (unchanged)
                ├── ModernBankRestAdapter  (unchanged)
                └── (Aggregation Step)
                    └── SnsNotificationAdapter  ← you will implement this (replaces TwilioNotificationAdapter)
                        └── AWS SNS Topic → Email / SMS to donors
```

---

## 5. Project Structure Changes

```text
blood-integration-broker/
  pom.xml                              ← ADD: AWS SDK v2 dependency
  src/
    main/
      java/com/wethinkcode/bloodbroker/
        config/
          SftpInboundConfig.java       ← REPLACE: implement as SQS poller
          WebServiceConfig.java        ← KEEP: no changes needed yet
        adapter/
          TwilioNotificationAdapter.java  ← UPDATE: publish to SNS
      resources/
        application.yml                ← POPULATE: add AWS region, queue URL, topic ARN
```

> **Do NOT modify** `BrokerApplication.java`, any test files, or any files in `transformer/`, `router/`, or `domain/`.

---

## 6. Implementation Steps

Work through these in order — each one builds on the previous.

---

### Step 2.1 — Add AWS SDK Dependency (pom.xml)

**File:** `pom.xml`

Add the AWS SDK v2 Bill of Materials (BOM) to your `<dependencyManagement>` section, then declare individual module dependencies.

**Modules you need:**
- `software.amazon.awssdk` → `sqs` (for polling the inbound queue)
- `software.amazon.awssdk` → `sns` (for publishing emergency alerts)
- `software.amazon.awssdk` → `auth` (handles credentials automatically via environment variables or IAM roles)

**Version to use:** `2.25.x` (latest stable at time of writing — check Maven Central for the newest patch version)

---

### Step 2.2 — Populate application.yml

**File:** `src/main/resources/application.yml`

Add the following configuration keys. These values should come from your AWS Console after completing Section 3 above.

| Key | Description | Example Value |
| :--- | :--- | :--- |
| `aws.region` | The AWS region where your SQS and SNS resources live | `af-south-1` |
| `aws.sqs.queue-url` | The full URL of the `blood-broker-inbound` SQS queue | `https://sqs.af-south-1.amazonaws.com/123456789/blood-broker-inbound` |
| `aws.sns.topic-arn` | The ARN of the `blood-broker-emergency-alerts` SNS topic | `arn:aws:sns:af-south-1:123456789:blood-broker-emergency-alerts` |
| `aws.sqs.poll-delay-ms` | How often (in milliseconds) to poll the queue for new messages | `5000` |

> **Security note:** Never commit real AWS credentials into `application.yml`. Credentials should come from environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) or an IAM role — the AWS SDK picks these up automatically.

---

### Step 2.3 — Implement SQS Inbound Config (SftpInboundConfig.java)

**File:** `src/main/java/com/wethinkcode/bloodbroker/config/SftpInboundConfig.java`

This class replaces the old SFTP file poller. Instead of watching a folder for new files, it now polls an SQS queue for new messages.

**What this class must do:**
- Annotate the class with `@Configuration` and `@EnableScheduling` so Spring manages it.
- Inject the `aws.sqs.queue-url` and `aws.sqs.poll-delay-ms` values from `application.yml` using `@Value`.
- Create an `SqsClient` bean configured with your AWS region. The client will use credentials from your environment automatically.
- Implement a `@Scheduled` method called `pollQueue()` that:
  1. Calls `SqsClient.receiveMessage()` to fetch up to 10 messages at a time from the queue.
  2. For each message received, extracts the message body (which is the raw XML string).
  3. Passes the XML string to `XmlToCanonicalTransformer.transform()` to convert it into a `HospitalRequest`.
  4. Passes the result to `PayloadEnricher.enrich()` to stamp it with a timestamp.
  5. Deletes the message from the queue using `SqsClient.deleteMessage()` so it is not processed again.
  6. Logs each step clearly so you can trace the flow.

**Fields it needs:**
- `queueUrl` (String) — injected from `application.yml`
- `pollDelayMs` (long) — injected from `application.yml`
- `sqsClient` (SqsClient) — autowired bean you define in this class
- `transformer` (XmlToCanonicalTransformer) — autowired
- `enricher` (PayloadEnricher) — autowired

---

### Step 2.4 — Update the Notification Adapter (TwilioNotificationAdapter.java)

**File:** `src/main/java/com/wethinkcode/bloodbroker/adapter/TwilioNotificationAdapter.java`

The method signature stays **exactly the same** — `public void triggerDonorAlert(String bloodType)` — so nothing that calls it needs to change.

**What changes inside the method:**
- Inject the `aws.sns.topic-arn` value from `application.yml` using `@Value`.
- Create an `SnsClient` bean configured with your AWS region.
- Replace the `log.warn(...)` simulation with a real `SnsClient.publish()` call.
- The message body should be a human-readable alert string, for example: `"URGENT: Blood type O-Negative is critically needed. Please donate today!"`
- Keep the `log.info(...)` confirmation line so you can still see what happened in the console.
- If the publish call fails, catch the exception, log the error, and re-throw it as a `RuntimeException` so the router knows something went wrong.

---

## 7. How to Test Your Changes

### Local Testing (Without Real AWS)

Before pushing to AWS, you can test the SQS integration locally using **LocalStack** — a tool that simulates AWS services on your machine.

1. Add LocalStack to your `docker-compose.yml` as a new service on port `4566`.
2. Point `aws.sqs.queue-url` in `application.yml` to `http://localhost:4566/000000000000/blood-broker-inbound`.
3. Run `docker-compose up -d` and then create the local queue using the AWS CLI pointed at LocalStack.
4. Send a test XML message to the local queue using the AWS CLI.
5. Watch your application logs — you should see the message picked up, transformed, enriched, and processed.

### Real AWS Testing

1. Run `mvn compile` to confirm there are no compilation errors.
2. Run `mvn package -DskipTests` to build the JAR.
3. Run the JAR with your AWS credentials exported as environment variables.
4. In the AWS Console, go to your SQS queue and click **Send and receive messages**.
5. Paste a valid XML hospital request into the message body and click **Send**.
6. Watch your application logs — you should see the message processed end-to-end.
7. Check your email — you should receive the emergency shortage alert from SNS.

---

## 8. Definition of Done

You have completed Step 2 when all of the following are true:

- [ ] `mvn compile` passes with zero errors.
- [ ] `mvn test` passes with all 20 tests green.
- [ ] `SftpInboundConfig.java` polls a real (or LocalStack) SQS queue and processes messages through the existing transformer and enricher pipeline.
- [ ] `TwilioNotificationAdapter.java` publishes to a real (or LocalStack) SNS topic instead of just logging.
- [ ] `application.yml` contains your AWS region, queue URL, and topic ARN (with no real credentials committed to source control).
- [ ] You can send a test message to the SQS queue and watch it flow through the entire pipeline in the application logs.
