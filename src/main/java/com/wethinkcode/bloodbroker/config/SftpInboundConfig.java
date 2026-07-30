package com.wethinkcode.bloodbroker.config;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import com.wethinkcode.bloodbroker.transformer.PayloadEnricher;
import com.wethinkcode.bloodbroker.transformer.XmlToCanonicalTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Step 2.3 — AWS SQS Inbound Configuration & Queue Poller
 *
 * Replaces the local SFTP file poller with an Amazon SQS Queue Poller.
 * Polls the SQS queue defined in application.yml for legacy hospital XML requests,
 * transforms and enriches them, and removes processed messages from the queue.
 */
@Configuration
@EnableScheduling
public class SftpInboundConfig {

    private static final Logger log = LoggerFactory.getLogger(SftpInboundConfig.class);

    @Value("${aws.region:eu-north-1}")
    private String awsRegion;

    @Value("${aws.sqs.queue-url:}")
    private String queueUrl;

    private final XmlToCanonicalTransformer transformer;
    private final PayloadEnricher enricher;
    private SqsClient sqsClient;

    public SftpInboundConfig(XmlToCanonicalTransformer transformer, PayloadEnricher enricher) {
        this.transformer = transformer;
        this.enricher = enricher;
    }

    @Bean
    public SqsClient sqsClient() {
        if (this.sqsClient == null) {
            this.sqsClient = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
        }
        return this.sqsClient;
    }

    /**
     * Polls the SQS queue periodically for incoming XML messages.
     */
    @Scheduled(fixedDelayString = "${aws.sqs.poll-delay-ms:5000}")
    public void pollQueue() {
        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }

        try {
            SqsClient client = sqsClient();
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .build();

            ReceiveMessageResponse response = client.receiveMessage(receiveRequest);
            for (Message message : response.messages()) {
                processMessage(client, message);
            }
        } catch (Exception e) {
            log.error("[SQS] Error polling queue '{}': {}", queueUrl, e.getMessage());
        }
    }

    private void processMessage(SqsClient client, Message message) {
        try {
            String xmlPayload = message.body();
            log.info("[SQS] Received message ID: {}", message.messageId());

            HospitalRequest request = transformer.transform(xmlPayload);
            HospitalRequest enrichedRequest = enricher.enrich(request);

            log.info("[SQS] Successfully processed request for hospital '{}', bloodType: '{}', units: {}",
                    enrichedRequest.getHospitalId(),
                    enrichedRequest.getBloodType(),
                    enrichedRequest.getUnitsRequired());

            // Delete processed message from queue
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            client.deleteMessage(deleteRequest);
            log.info("[SQS] Deleted message ID '{}' from queue.", message.messageId());

        } catch (Exception e) {
            log.error("[SQS] Failed to process message ID '{}': {}", message.messageId(), e.getMessage(), e);
        }
    }
}
