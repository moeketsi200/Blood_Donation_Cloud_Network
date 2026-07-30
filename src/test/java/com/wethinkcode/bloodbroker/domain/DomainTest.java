package com.wethinkcode.bloodbroker.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    // ── HospitalRequest ───────────────────────────────────────────────────────

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
    void testHospitalRequestAllArgsConstructor() {
        HospitalRequest request = new HospitalRequest("AB-Positive", 2, "HOSP-XYZ");

        assertEquals("AB-Positive", request.getBloodType());
        assertEquals(2, request.getUnitsRequired());
        assertEquals("HOSP-XYZ", request.getHospitalId());
        assertNull(request.getTimestamp());
    }

    // ── BloodInventoryStatus ─────────────────────────────────────────────────

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
    void testBloodInventoryStatusAllArgsConstructor() {
        BloodInventoryStatus status = new BloodInventoryStatus("Bank C", 7, false);

        assertEquals("Bank C", status.getBankName());
        assertEquals(7, status.getUnitsAvailable());
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

    // ── EmergencyAlert ───────────────────────────────────────────────────────

    @Test
    void testEmergencyAlertConstructorAndGetters() {
        EmergencyAlert alert = new EmergencyAlert("O-Negative", "HOSP-007", "2024-01-01T00:00:00Z", 5);

        assertEquals("O-Negative", alert.getBloodType());
        assertEquals("HOSP-007", alert.getHospitalId());
        assertEquals("2024-01-01T00:00:00Z", alert.getTimestamp());
        assertEquals(5, alert.getUnitsRequired());
    }

    @Test
    void testEmergencyAlertSetters() {
        EmergencyAlert alert = new EmergencyAlert("A-Positive", "HOSP-001", "2024-06-01T12:00:00Z", 3);

        alert.setBloodType("B-Negative");
        alert.setHospitalId("HOSP-999");
        alert.setTimestamp("2025-01-01T00:00:00Z");
        alert.setUnitsRequired(10);

        assertEquals("B-Negative", alert.getBloodType());
        assertEquals("HOSP-999", alert.getHospitalId());
        assertEquals("2025-01-01T00:00:00Z", alert.getTimestamp());
        assertEquals(10, alert.getUnitsRequired());
    }
}
