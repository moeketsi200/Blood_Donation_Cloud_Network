package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Step 2b — Payload Enrichment
 *
 * Responsibility: Add missing metadata to the canonical {@link HospitalRequest}
 * AFTER it has been transformed from XML.
 *
 * This keeps enrichment logic out of the transformer and follows the
 * Single Responsibility Principle — each class does exactly one thing.
 *
 * What it enriches:
 *  - timestamp: injects the current UTC time (ISO-8601) so downstream systems
 *    always have a reliable processing timestamp, regardless of whether the
 *    legacy system provided one.
 */
@Component
public class PayloadEnricher {

    /**
     * Enriches the given {@link HospitalRequest} by stamping it with the
     * current UTC time if no timestamp is already present.
     *
     * @param request the canonical request produced by {@link XmlToCanonicalTransformer}
     * @return the same request object, now populated with a timestamp
     */
    public HospitalRequest enrich(HospitalRequest request) {
        // Only add a timestamp when none exists — avoids overwriting a timestamp
        // that was present in the original XML.
        if (request.getTimestamp() == null || request.getTimestamp().isBlank()) {
            request.setTimestamp(Instant.now().toString());
        }
        return request;
    }
}
