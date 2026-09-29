package com.wethinkcode.bloodbroker.controller;

import com.wethinkcode.bloodbroker.adapter.LegacyBankSoapAdapter;
import com.wethinkcode.bloodbroker.adapter.ModernBankRestAdapter;
import com.wethinkcode.bloodbroker.adapter.TwilioNotificationAdapter;
import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import com.wethinkcode.bloodbroker.router.BloodBankRouter;
import com.wethinkcode.bloodbroker.transformer.PayloadEnricher;
import com.wethinkcode.bloodbroker.transformer.XmlToCanonicalTransformer;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.*;

@RestController
@RequestMapping("/api/broker")
@CrossOrigin(origins = "*")
public class BrokerRestController {

    private static final Logger log = LoggerFactory.getLogger(BrokerRestController.class);

    private final XmlToCanonicalTransformer transformer;
    private final PayloadEnricher enricher;
    private final LegacyBankSoapAdapter soapAdapter;
    private final ModernBankRestAdapter restAdapter;
    private final BloodBankRouter router;
    private final TwilioNotificationAdapter notificationAdapter;
    private final BloodInventoryRepository inventoryRepository;

    @Value("${aws.region:eu-north-1}")
    private String awsRegion;

    @Value("${aws.sqs.queue-url:}")
    private String queueUrl;

    public BrokerRestController(BrokerDependencies dependencies,
                                BloodInventoryRepository inventoryRepository) {
        this.transformer = dependencies.getTransformer();
        this.enricher = dependencies.getEnricher();
        this.soapAdapter = dependencies.getSoapAdapter();
        this.restAdapter = dependencies.getRestAdapter();
        this.router = dependencies.getRouter();
        this.notificationAdapter = dependencies.getNotificationAdapter();
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Executes the EIP Integration Pipeline synchronously for real-time dashboard simulation.
     */
    @PostMapping("/process-xml")
    public ResponseEntity<Map<String, Object>> processXmlPayload(@RequestBody String rawXml) {
        Map<String, Object> trace = new LinkedHashMap<>();
        try {
            trace.put("step1_raw_xml", rawXml);

            // Step 1: Data Transformation (XML -> Canonical Java Object)
            HospitalRequest request = transformer.transform(rawXml);
            trace.put("step2_transformed_canonical", request);

            // Step 2: Payload Enrichment (Timestamping)
            HospitalRequest enriched = enricher.enrich(request);
            trace.put("step3_enriched_canonical", enriched);

            // Step 3: Outbound Adapters (SOAP & REST)
            BloodInventoryStatus bankA = soapAdapter.checkStock(enriched);
            BloodInventoryStatus bankB = restAdapter.queryInventory(enriched);

            List<BloodInventoryStatus> bankReplies = List.of(bankA, bankB);
            trace.put("step4_adapter_replies", bankReplies);

            // Step 4: Scatter-Gather Aggregation
            BloodInventoryStatus aggregated = router.aggregate(bankReplies);
            trace.put("step5_aggregated_status", aggregated);

            // Step 4.5: Save to Database
            aggregated.setBloodType(enriched.getBloodType());
            inventoryRepository.save(aggregated);
            trace.put("step6_database_save", "Saved aggregated stock to DynamoDB Table 'BloodInventory'");

            // Step 5: Event Trigger (SMS / SNS on emergency shortage)
            boolean isEmergency = aggregated.isEmergencyShortage();
            trace.put("emergency_alert_triggered", isEmergency);

            if (isEmergency) {
                notificationAdapter.triggerDonorAlert(enriched.getBloodType());
                trace.put("notification_status", "EMERGENCY_SNS_ALERT_DISPATCHED");
            } else {
                trace.put("notification_status", "STOCK_AVAILABLE_NO_ALERT");
            }

            trace.put("status", "SUCCESS");
            return ResponseEntity.ok(trace);

        } catch (Exception e) {
            log.error("Pipeline execution error: {}", e.getMessage(), e);
            trace.put("status", "ERROR");
            trace.put("error_message", e.getMessage());
            return ResponseEntity.badRequest().body(trace);
        }
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveInventory(@RequestBody String rawXml) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            HospitalRequest request = transformer.transform(rawXml);
            int requestedUnits = request.getUnitsRequired();
            int reserved = inventoryRepository.reserveInventory(request.getBloodType(), requestedUnits);
            
            response.put("status", "SUCCESS");
            response.put("reserved_units", reserved);
            response.put("requested_units", requestedUnits);
            response.put("blood_type", request.getBloodType());
            response.put("message", "Successfully reserved " + reserved + " units of " + request.getBloodType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reserve inventory: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/supply")
    public ResponseEntity<Map<String, Object>> supplyInventory(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            String bankName = (String) payload.getOrDefault("bankName", "Local Blood Drive");
            String bloodType = (String) payload.getOrDefault("bloodType", "O-Positive");
            int units = Integer.parseInt(payload.getOrDefault("units", "1").toString());

            BloodInventoryStatus existing = inventoryRepository.getStatus(bankName, bloodType);
            if (existing != null) {
                existing.setUnitsAvailable(existing.getUnitsAvailable() + units);
                existing.setEmergencyShortage(existing.getUnitsAvailable() == 0);
                inventoryRepository.save(existing);
            } else {
                BloodInventoryStatus newBank = new BloodInventoryStatus(bankName, bloodType, units, false);
                inventoryRepository.save(newBank);
            }

            response.put("status", "SUCCESS");
            response.put("message", "Successfully added " + units + " units of " + bloodType + " to " + bankName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to supply inventory: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Publishes a raw XML message directly to the AWS SQS Queue.
     */
    @PostMapping("/sqs/publish")
    public ResponseEntity<Map<String, Object>> publishToSqs(@RequestBody String rawXml) {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        try {
            if (queueUrl == null || queueUrl.isBlank()) {
                throw new IllegalStateException("AWS SQS Queue URL is not configured.");
            }

            SqsClient sqsClient = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .build();

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(rawXml)
                    .build();

            SendMessageResponse sendMsgResponse = sqsClient.sendMessage(sendMsgRequest);

            responseMap.put("status", "SUCCESS");
            responseMap.put("message_id", sendMsgResponse.messageId());
            responseMap.put("queue_url", queueUrl);
            responseMap.put("region", awsRegion);
            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("Failed to publish message to SQS: {}", e.getMessage(), e);
            responseMap.put("status", "ERROR");
            responseMap.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(responseMap);
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getDatabaseInventory() {
        try {
            return ResponseEntity.ok(inventoryRepository.getAllInventory());
        } catch (Exception e) {
            log.error("Failed to fetch inventory from DynamoDB: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns system telemetry & live infrastructure status for the dashboard header.
     */
    @GetMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> getTelemetry() {
        Map<String, Object> telemetry = new LinkedHashMap<>();
        telemetry.put("broker_name", "Blood Integration Broker");
        telemetry.put("version", "1.0-SNAPSHOT");
        telemetry.put("environment", "AWS Cloud Fargate");
        telemetry.put("aws_region", awsRegion);
        telemetry.put("sqs_queue", queueUrl);
        telemetry.put("connected_adapters", List.of("LegacyBankSoapAdapter (Bank A)", "ModernBankRestAdapter (Bank B)", "TwilioNotificationAdapter (SNS)", "DynamoDB (AWS Data Store)"));
        telemetry.put("uptime_status", "HEALTHY_ONLINE");
        telemetry.put("server_time", new Date().toString());
        return ResponseEntity.ok(telemetry);
    }
}
