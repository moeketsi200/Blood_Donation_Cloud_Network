package com.wethinkcode.bloodbroker.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    @Test
    void testHospitalRequest() {
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("O-Negative");
        request.setUnitsRequired(10);
        request.setHospitalId("HOSP-123");
        request.setTimestamp("2023-10-01T10:00:00Z");

        assertEquals("O-Negative", request.getBloodType());
        assertEquals(10, request.getUnitsRequired());
        assertEquals("HOSP-123", request.getHospitalId());
        assertEquals("2023-10-01T10:00:00Z", request.getTimestamp());
    }

    @Test
    void testBloodInventoryStatus() {
        BloodInventoryStatus status = new BloodInventoryStatus();
        status.setBankName("Bank A");
        status.setUnitsAvailable(5);
        status.setEmergencyShortage(false);

        assertEquals("Bank A", status.getBankName());
        assertEquals(5, status.getUnitsAvailable());
        assertFalse(status.isEmergencyShortage());
    }

    @Test
    void testBloodInventoryStatusEmergencyShortageLogic() {
        BloodInventoryStatus status = new BloodInventoryStatus();
        
        status.setUnitsAvailable(0);
        assertTrue(status.isEmergencyShortage(), "Should be an emergency shortage when units are 0");

        status.setUnitsAvailable(1);
        assertFalse(status.isEmergencyShortage(), "Should not be an emergency shortage when units > 0");
    }
}
