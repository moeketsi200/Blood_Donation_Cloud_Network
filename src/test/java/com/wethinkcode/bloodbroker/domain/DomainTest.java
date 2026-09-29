package com.wethinkcode.bloodbroker.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    // ── Custom Assertions ────────────────────────────────────────────────────
    
    private void assertHospitalRequest(HospitalRequest request, String expectedType, int expectedUnits, String expectedHospital, String expectedTimestamp) {
        assertEquals(expectedType, request.getBloodType());
        assertEquals(expectedUnits, request.getUnitsRequired());
        assertEquals(expectedHospital, request.getHospitalId());
        assertEquals(expectedTimestamp, request.getTimestamp());
    }

    private void assertBloodInventoryStatus(BloodInventoryStatus status, String expectedBank, String expectedType, int expectedUnits, boolean expectedShortage) {
        assertEquals(expectedBank, status.getBankName());
        assertEquals(expectedType, status.getBloodType());
        assertEquals(expectedUnits, status.getUnitsAvailable());
        assertEquals(expectedShortage, status.isEmergencyShortage());
    }

    private void assertEmergencyAlert(EmergencyAlert alert, String expectedType, String expectedHospital, String expectedTimestamp, int expectedUnits) {
        assertEquals(expectedType, alert.getBloodType());
        assertEquals(expectedHospital, alert.getHospitalId());
        assertEquals(expectedTimestamp, alert.getTimestamp());
        assertEquals(expectedUnits, alert.getUnitsRequired());
    }

    // ── HospitalRequest ───────────────────────────────────────────────────────

    @Test
    void testHospitalRequest() {
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("O-Negative");
        request.setUnitsRequired(10);
        request.setHospitalId("HOSP-123");
        request.setTimestamp("2023-10-01T10:00:00Z");

        assertHospitalRequest(request, "O-Negative", 10, "HOSP-123", "2023-10-01T10:00:00Z");
    }

    @Test
    void testHospitalRequestAllArgsConstructor() {
        HospitalRequest request = new HospitalRequest("AB-Positive", 2, "HOSP-XYZ");
        assertHospitalRequest(request, "AB-Positive", 2, "HOSP-XYZ", null);
    }

    // ── BloodInventoryStatus ─────────────────────────────────────────────────

    @Test
    void testBloodInventoryStatus() {
        BloodInventoryStatus status = new BloodInventoryStatus();
        status.setBankName("Bank A");
        status.setUnitsAvailable(5);
        status.setEmergencyShortage(false);

        assertBloodInventoryStatus(status, "Bank A", "UNKNOWN", 5, false);
    }

    @Test
    void testBloodInventoryStatusAllArgsConstructor() {
        BloodInventoryStatus status = new BloodInventoryStatus("Bank C", "O-Positive", 7, false);
        assertBloodInventoryStatus(status, "Bank C", "O-Positive", 7, false);
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
        assertEmergencyAlert(alert, "O-Negative", "HOSP-007", "2024-01-01T00:00:00Z", 5);
    }

    @Test
    void testEmergencyAlertSetters() {
        EmergencyAlert alert = new EmergencyAlert("A-Positive", "HOSP-001", "2024-06-01T12:00:00Z", 3);

        alert.setBloodType("B-Negative");
        alert.setHospitalId("HOSP-999");
        alert.setTimestamp("2025-01-01T00:00:00Z");
        alert.setUnitsRequired(10);

        assertEmergencyAlert(alert, "B-Negative", "HOSP-999", "2025-01-01T00:00:00Z", 10);
    }
}
