package com.wethinkcode.bloodbroker.repository;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class BloodInventoryRepository {

    private final DynamoDbTable<BloodInventoryStatus> inventoryTable;

    public BloodInventoryRepository(DynamoDbEnhancedClient enhancedClient) {
        this.inventoryTable = enhancedClient.table("BloodInventory", TableSchema.fromBean(BloodInventoryStatus.class));
    }

    public void save(BloodInventoryStatus status) {
        inventoryTable.putItem(status);
    }

    public BloodInventoryStatus getStatus(String bankName, String bloodType) {
        return inventoryTable.getItem(r -> r.key(k -> k.partitionValue(bankName).sortValue(bloodType)));
    }

    public int reserveInventory(String bloodType, int requestedUnits) {
        List<BloodInventoryStatus> allBanks = getAllInventory().stream()
                .filter(b -> b.getBloodType().equals(bloodType) && b.getUnitsAvailable() > 0)
                .collect(Collectors.toList());

        int unitsToFulfill = requestedUnits;
        for (BloodInventoryStatus bank : allBanks) {
            if (unitsToFulfill <= 0) break;
            
            int available = bank.getUnitsAvailable();
            if (available >= unitsToFulfill) {
                bank.setUnitsAvailable(available - unitsToFulfill);
                unitsToFulfill = 0;
            } else {
                bank.setUnitsAvailable(0);
                unitsToFulfill -= available;
            }
            save(bank);
        }
        
        return requestedUnits - unitsToFulfill; // return number of units successfully reserved
    }

    public List<BloodInventoryStatus> getAllInventory() {
        return inventoryTable.scan().items().stream().collect(Collectors.toList());
    }
}
