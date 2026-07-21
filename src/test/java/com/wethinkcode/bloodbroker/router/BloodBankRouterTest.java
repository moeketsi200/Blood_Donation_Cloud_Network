package com.wethinkcode.bloodbroker.router;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BloodBankRouterTest {

    @Test
    void testAggregateWithAvailableStock() {
        BloodBankRouter router = new BloodBankRouter();
        
        BloodInventoryStatus bankA = new BloodInventoryStatus();
        bankA.setBankName("Legacy Bank A");
        bankA.setUnitsAvailable(4);

        BloodInventoryStatus bankB = new BloodInventoryStatus();
        bankB.setBankName("Modern Bank B");
        bankB.setUnitsAvailable(6);

        List<BloodInventoryStatus> replies = Arrays.asList(bankA, bankB);
        
        BloodInventoryStatus aggregated = router.aggregate(replies);
        
        assertNotNull(aggregated);
        assertEquals(10, aggregated.getUnitsAvailable());
        assertFalse(aggregated.isEmergencyShortage(), "Should not flag emergency if total stock > 0");
    }

    @Test
    void testAggregateWithEmergencyShortage() {
        BloodBankRouter router = new BloodBankRouter();
        
        BloodInventoryStatus bankA = new BloodInventoryStatus();
        bankA.setBankName("Legacy Bank A");
        bankA.setUnitsAvailable(0);

        BloodInventoryStatus bankB = new BloodInventoryStatus();
        bankB.setBankName("Modern Bank B");
        bankB.setUnitsAvailable(0);

        List<BloodInventoryStatus> replies = Arrays.asList(bankA, bankB);
        
        BloodInventoryStatus aggregated = router.aggregate(replies);
        
        assertNotNull(aggregated);
        assertEquals(0, aggregated.getUnitsAvailable());
        assertTrue(aggregated.isEmergencyShortage(), "Should flag emergency if total stock == 0");
    }
}
