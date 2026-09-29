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

    public BrokerRestController(XmlToCanonicalTransformer transformer,
                                PayloadEnricher enricher,
                                LegacyBankSoapAdapter soapAdapter,
                                ModernBankRestAdapter restAdapter,
                                BloodBankRouter router,
                                TwilioNotificationAdapter notificationAdapter,
                                BloodInventoryRepository inventoryRepository) {
        this.transformer = transformer;
        this.enricher = enricher;
        this.soapAdapter = soapAdapter;
        this.restAdapter = restAdapter;
        this.router = router;
        this.notificationAdapter = notificationAdapter;
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

            // Step 4.5: Save to Database & Reserve Inventory
            aggregated.setBloodType(enriched.getBloodType());
            inventoryRepository.save(aggregated);
            trace.put("step6_database_save", "Saved aggregated stock to DynamoDB Table 'BloodInventory'");

            // Step 4.75: Actual Inventory Reservation!
            int requestedUnits = enriched.getUnitsRequired();
            int reserved = inventoryRepository.reserveInventory(enriched.getBloodType(), requestedUnits);
            trace.put("step7_inventory_reservation", "Reserved " + reserved + " of " + requestedUnits + " requested units across AWS banks.");

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
