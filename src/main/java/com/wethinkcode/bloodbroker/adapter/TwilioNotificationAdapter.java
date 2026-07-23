package com.wethinkcode.bloodbroker.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3c — Outbound Adapter (Event Trigger: SMS / Twilio)
 *
 * Responsibility: Notify blood donors when no blood bank can satisfy a
 * hospital's request (emergency shortage).
 *
 * Enterprise Integration Pattern — Event-Driven Consumer:
 *  This adapter is triggered only when the aggregated result from the
 *  scatter-gather step reports an emergency shortage (total units == 0).
 *
 * Twilio integration:
 *  In a production system this would call the Twilio REST API (via the
 *  Twilio Java SDK) to send an SMS to registered donors whose blood type
 *  matches the shortage.
 *
 *  The Twilio SDK is on the classpath (see pom.xml). Real usage looks like:
 *
 *    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
 *    Message.creator(
 *        new PhoneNumber("+27821234567"),          // donor's number
 *        new PhoneNumber(twilioFromNumber),
 *        "URGENT: We need " + bloodType + " blood. Please donate today!"
 *    ).create();
 *
 *  For this broker we log the alert to avoid requiring real credentials.
 */
@Component
public class TwilioNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(TwilioNotificationAdapter.class);

    /**
     * Triggers a donor alert for the given blood type.
     *
     * Called by the router when the aggregated stock for a blood type is zero,
     * indicating an emergency shortage across all connected blood banks.
     *
     * @param bloodType the blood type that is critically short (e.g., "O-Negative")
     */
    public void triggerDonorAlert(String bloodType) {
        // ── Log the alert (simulates the SMS in the absence of real credentials) ──
        log.warn("🚨 EMERGENCY SHORTAGE ALERT: Blood type '{}' is at ZERO across all banks. "
               + "Triggering donor notification via Twilio SMS...", bloodType);

        // ── In production: initialise Twilio and send SMS ──────────────────────
        // String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        // String authToken  = System.getenv("TWILIO_AUTH_TOKEN");
        // String fromNumber = System.getenv("TWILIO_FROM_NUMBER");
        //
        // Twilio.init(accountSid, authToken);
        // for (String donorNumber : donorRegistryService.getDonorsForBloodType(bloodType)) {
        //     Message.creator(
        //         new PhoneNumber(donorNumber),
        //         new PhoneNumber(fromNumber),
        //         "URGENT: " + bloodType + " blood is critically needed. Please donate today!"
        //     ).create();
        // }

        log.info("✅ Donor alert dispatched for blood type '{}'.", bloodType);
    }
}
