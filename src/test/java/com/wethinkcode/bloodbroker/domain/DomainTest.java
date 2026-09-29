package com.wethinkcode.bloodbroker.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    // ── Custom Assertions ────────────────────────────────────────────────────
    
    private void assertHospitalRequest(HospitalRequest actual, HospitalRequest expected) {
        assertEquals(expected.getBloodType(), actual.getBloodType());
        assertEquals(expected.getUnitsRequired(), actual.getUnitsRequired());
        assertEquals(expected.getHospitalId(), actual.getHospitalId());
        assertEquals(expected.getTimestamp(), actual.getTimestamp());
    }

    private void assertBloodInventoryStatus(BloodInventoryStatus actual, BloodInventoryStatus expected) {
        assertEquals(expected.getBankName(), actual.getBankName());
        assertEquals(expected.getBloodType(), actual.getBloodType());
        assertEquals(expected.getUnitsAvailable(), actual.getUnitsAvailable());
        assertEquals(expected.isEmergencyShortage(), actual.isEmergencyShortage());
    }

    private void assertEmergencyAlert(EmergencyAlert actual, EmergencyAlert expected) {
        assertEquals(expected.getBloodType(), actual.getBloodType());
        assertEquals(expected.getHospitalId(), actual.getHospitalId());
        assertEquals(expected.getTimestamp(), actual.getTimestamp());
        assertEquals(expected.getUnitsRequired(), actual.getUnitsRequired());
    }

    // ── HospitalRequest ───────────────────────────────────────────────────────

    @Test
    void testHospitalRequest() {
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("O-Negative");
        request.setUnitsRequired(10);
        request.setHospitalId("HOSP-123");
        request.setTimestamp("2023-10-01T10:00:00Z");

        HospitalRequest expected = new HospitalRequest();
        expected.setBloodType("O-Negative");
        expected.setUnitsRequired(10);
        expected.setHospitalId("HOSP-123");
        expected.setTimestamp("2023-10-01T10:00:00Z");

        assertHospitalRequest(request, expected);
    }

    @Test
    void testHospitalRequestAllArgsConstructor() {
        HospitalRequest request = new HospitalRequest("AB-Positive", 2, "HOSP-XYZ");
        
        HospitalRequest expected = new HospitalRequest();
        expected.setBloodType("AB-Positive");
        expected.setUnitsRequired(2);
        expected.setHospitalId("HOSP-XYZ");
        
        assertHospitalRequest(request, expected);
    }

    // ── BloodInventoryStatus ─────────────────────────────────────────────────

    @Test
    void testBloodInventoryStatus() {
        BloodInventoryStatus status = new BloodInventoryStatus();
        status.setBankName("Bank A");
        status.setUnitsAvailable(5);
        status.setEmergencyShortage(false);

        BloodInventoryStatus expected = new BloodInventoryStatus();
        expected.setBankName("Bank A");
        expected.setUnitsAvailable(5);
        expected.setEmergencyShortage(false);

        assertBloodInventoryStatus(status, expected);
    }

    @Test
    void testBloodInventoryStatusAllArgsConstructor() {
        BloodInventoryStatus status = new BloodInventoryStatus("Bank C", "O-Positive", 7, false);
        BloodInventoryStatus expected = new BloodInventoryStatus("Bank C", "O-Positive", 7, false);
        
        assertBloodInventoryStatus(status, expected);
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
        EmergencyAlert expected = new EmergencyAlert("O-Negative", "HOSP-007", "2024-01-01T00:00:00Z", 5);
        
        assertEmergencyAlert(alert, expected);
    }

    @Test
    void testEmergencyAlertSetters() {
        EmergencyAlert alert = new EmergencyAlert("A-Positive", "HOSP-001", "2024-06-01T12:00:00Z", 3);

        alert.setBloodType("B-Negative");
        alert.setHospitalId("HOSP-999");
        alert.setTimestamp("2025-01-01T00:00:00Z");
        alert.setUnitsRequired(10);

        EmergencyAlert expected = new EmergencyAlert("B-Negative", "HOSP-999", "2025-01-01T00:00:00Z", 10);
        assertEmergencyAlert(alert, expected);
    }
}
