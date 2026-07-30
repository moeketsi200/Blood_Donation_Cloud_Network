package com.wethinkcode.bloodbroker.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * Step 2.4 — Outbound Adapter (Event Trigger: AWS SNS / Donor Alerts)
 *
 * Responsibility: Notify blood donors via Amazon SNS when no blood bank can satisfy a
 * hospital's request (emergency shortage).
 */
@Component
public class TwilioNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(TwilioNotificationAdapter.class);

    @Value("${aws.region:eu-north-1}")
    private String awsRegion;

    @Value("${aws.sns.topic-arn:}")
    private String topicArn;

    private SnsClient snsClient;

    public void setSnsClient(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    private SnsClient getSnsClient() {
        if (this.snsClient == null) {
            this.snsClient = SnsClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
        }
        return this.snsClient;
    }

    /**
     * Triggers a donor alert for the given blood type by publishing to Amazon SNS.
     *
     * @param bloodType the blood type that is critically short (e.g., "O-Negative")
     */
    public void triggerDonorAlert(String bloodType) {
        log.warn("🚨 EMERGENCY SHORTAGE ALERT: Blood type '{}' is at ZERO across all banks. "
               + "Triggering donor notification...", bloodType);

        String alertMessage = "URGENT: Blood type " + bloodType + " is critically needed. Please donate today!";

        if (topicArn != null && !topicArn.isBlank()) {
            try {
                PublishRequest publishRequest = PublishRequest.builder()
                        .topicArn(topicArn)
                        .message(alertMessage)
                        .subject("EMERGENCY BLOOD SHORTAGE: " + bloodType)
                        .build();

                PublishResponse response = getSnsClient().publish(publishRequest);
                log.info("📢 SNS Emergency alert published to topic '{}'. Message ID: {}", topicArn, response.messageId());
            } catch (Exception e) {
                log.error("❌ Failed to publish SNS emergency alert for blood type '{}': {}", bloodType, e.getMessage(), e);
                throw new RuntimeException("Failed to send emergency SNS alert for blood type: " + bloodType, e);
            }
        } else {
            log.info("ℹ️ AWS SNS Topic ARN not set — skipping SNS publish step.");
        }

        log.info("✅ Donor alert dispatched for blood type '{}'.", bloodType);
    }
}
