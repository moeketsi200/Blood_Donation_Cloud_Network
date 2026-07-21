package com.wethinkcode.bloodbroker.adapter;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdapterTest {

    @Test
    void testLegacyBankSoapAdapter() {
        LegacyBankSoapAdapter adapter = new LegacyBankSoapAdapter();
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("O-Negative");

        BloodInventoryStatus status = adapter.checkStock(request);
        
        assertNotNull(status, "Adapter should return a mock status object");
        assertNotNull(status.getBankName(), "Bank name should be populated");
    }

    @Test
    void testModernBankRestAdapter() {
        ModernBankRestAdapter adapter = new ModernBankRestAdapter();
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("A-Positive");

        BloodInventoryStatus status = adapter.queryInventory(request);
        
        assertNotNull(status, "Adapter should return a mock status object");
        assertNotNull(status.getBankName(), "Bank name should be populated");
    }

    @Test
    void testTwilioNotificationAdapter() {
        TwilioNotificationAdapter adapter = new TwilioNotificationAdapter();
        // Since this method returns void and simulates an SMS, we verify it executes without error.
        assertDoesNotThrow(() -> {
            adapter.triggerDonorAlert("O-Negative");
        });
    }
}
