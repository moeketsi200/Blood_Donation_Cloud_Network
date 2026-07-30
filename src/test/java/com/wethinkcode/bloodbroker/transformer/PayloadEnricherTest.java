package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PayloadEnricherTest {

    private final PayloadEnricher enricher = new PayloadEnricher();

    @Test
    void testEnrichAddsTimestampWhenAbsent() {
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("B-Negative");
        request.setUnitsRequired(2);
        request.setHospitalId("HOSP-789");

        assertNull(request.getTimestamp());

        HospitalRequest enriched = enricher.enrich(request);

        assertNotNull(enriched);
        assertNotNull(enriched.getTimestamp(), "Enricher should add a timestamp when none exists");
        assertEquals("B-Negative", enriched.getBloodType());
        assertEquals(2, enriched.getUnitsRequired());
        assertEquals("HOSP-789", enriched.getHospitalId());
    }

    @Test
    void testEnrichDoesNotOverwriteExistingTimestamp() {
        // The if-branch: timestamp already present → must NOT be replaced
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("A-Positive");
        request.setUnitsRequired(4);
        request.setHospitalId("HOSP-001");
        request.setTimestamp("2024-01-15T08:30:00Z");

        HospitalRequest enriched = enricher.enrich(request);

        assertEquals("2024-01-15T08:30:00Z", enriched.getTimestamp(),
                "Enricher must preserve an existing timestamp");
    }

    @Test
    void testEnrichReplacesBlankTimestamp() {
        // Blank string counts as "absent" — must be replaced with a real timestamp
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("O-Positive");
        request.setUnitsRequired(1);
        request.setHospitalId("HOSP-002");
        request.setTimestamp("   ");

        HospitalRequest enriched = enricher.enrich(request);

        assertNotNull(enriched.getTimestamp());
        assertFalse(enriched.getTimestamp().isBlank(),
                "Enricher should replace a blank timestamp with a real one");
    }
}
