package com.wethinkcode.bloodbroker.transformer;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PayloadEnricherTest {

    @Test
    void testEnrich() {
        PayloadEnricher enricher = new PayloadEnricher();
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("B-Negative");
        request.setUnitsRequired(2);
        request.setHospitalId("HOSP-789");

        assertNull(request.getTimestamp());

        HospitalRequest enriched = enricher.enrich(request);

        assertNotNull(enriched);
        assertNotNull(enriched.getTimestamp(), "Enricher should add a timestamp");
        assertEquals("B-Negative", enriched.getBloodType());
        assertEquals(2, enriched.getUnitsRequired());
        assertEquals("HOSP-789", enriched.getHospitalId());
    }
}
