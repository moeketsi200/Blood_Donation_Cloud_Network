package com.wethinkcode.bloodbroker.adapter;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3a — Outbound Adapter (Protocol Translation: SOAP)
 *
 * This adapter represents the connection to Blood Bank A, which exposes a
 * legacy SOAP (XML over HTTP) web service.
 *
 * Enterprise Integration Pattern — Adapter / Translator:
 *  Wraps an external protocol (SOAP) behind a plain Java interface so the
 *  rest of the application never needs to know how Bank A communicates.
 *
 * In a production system this would:
 *  1. Build a SOAP XML envelope from the HospitalRequest.
 *  2. POST it to the SOAP endpoint (e.g., using Spring-WS WebServiceTemplate).
 *  3. Parse the SOAP response envelope back into a BloodInventoryStatus.
 *
 * For this integration broker the call is simulated (mocked) so that the
 * pipeline can be tested without a live SOAP server.
 */
@Component
public class LegacyBankSoapAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyBankSoapAdapter.class);

    /**
     * Checks the blood stock at Blood Bank A (Legacy SOAP system).
     *
     * @param request the canonical hospital request containing blood type and units needed
     * @return a {@link BloodInventoryStatus} representing Bank A's response
     */
    public BloodInventoryStatus checkStock(HospitalRequest request) {
        log.info("[SOAP] Querying Legacy Bank A for blood type '{}' — hospital: {}",
                 request.getBloodType(), request.getHospitalId());

        // ── Simulated SOAP response from Bank A ───────────────────────────────
        // In production: build SOAP envelope → send → parse response XML.
        // Here we return a mock response to keep the broker self-contained.
        BloodInventoryStatus status = new BloodInventoryStatus();
        status.setBankName("Legacy Bank A");
        status.setUnitsAvailable(5);          // mock: Bank A has 5 units on hand
        status.setEmergencyShortage(false);

        log.info("[SOAP] Legacy Bank A responded: {} units available", status.getUnitsAvailable());
        return status;
    }
}
