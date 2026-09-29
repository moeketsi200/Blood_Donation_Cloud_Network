package com.wethinkcode.bloodbroker.adapter;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;

/**
 * Step 3b — Outbound Adapter (Protocol Translation: REST)
 *
 * This adapter represents the connection to Blood Bank B, which exposes a
 * modern REST/JSON HTTP API.
 *
 * Enterprise Integration Pattern — Adapter / Translator:
 *  Wraps the REST protocol behind a plain Java interface so the scatter-gather
 *  router can call both Bank A (SOAP) and Bank B (REST) the same way.
 *
 * In a production system this would:
 *  1. Serialize the HospitalRequest to a JSON body.
 *  2. HTTP POST to the REST endpoint (e.g., using RestTemplate or WebClient).
 *  3. Deserialize the JSON response into a BloodInventoryStatus.
 *
 * For this broker the HTTP call is simulated (mocked) to keep it self-contained.
 */
@Component
public class ModernBankRestAdapter {

    private static final Logger log = LoggerFactory.getLogger(ModernBankRestAdapter.class);

    private final BloodInventoryRepository repository;

    public ModernBankRestAdapter(BloodInventoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Queries the blood inventory at Blood Bank B (Modern REST service).
     *
     * @param request the canonical hospital request containing blood type and units needed
     * @return a {@link BloodInventoryStatus} representing Bank B's response
     */
    public BloodInventoryStatus queryInventory(HospitalRequest request) {
        log.info("[REST] Querying Modern Bank B for blood type '{}' — hospital: {}",
                 request.getBloodType(), request.getHospitalId());

        // ── Simulated REST/JSON response from Bank B ──────────────────────────
        // In production: POST JSON → receive JSON → deserialize with Jackson.
        BloodInventoryStatus status = new BloodInventoryStatus();
        status.setBankName("Modern Bank B");
        
        try {
            // Attempt to fetch real live data from AWS DynamoDB!
            BloodInventoryStatus realStatus = repository.getStatus("Modern Bank B", request.getBloodType());
            if (realStatus != null) {
                status.setUnitsAvailable(realStatus.getUnitsAvailable());
                log.info("[REST] Successfully fetched live data from AWS DynamoDB!");
            } else {
                status.setUnitsAvailable(0);
                log.info("[REST] AWS DynamoDB has no record for this bank. Defaulting to 0.");
            }
        } catch (Exception e) {
            // If AWS credentials aren't set or internet is down, fallback to mock data
            log.error("[REST] Failed to connect to AWS DynamoDB: {}. Falling back to mock data.", e.getMessage());
            status.setUnitsAvailable(3); // fallback mock
        }
        
        status.setEmergencyShortage(status.getUnitsAvailable() == 0);

        log.info("[REST] Modern Bank B responded: {} units available", status.getUnitsAvailable());
        return status;
    }
}
