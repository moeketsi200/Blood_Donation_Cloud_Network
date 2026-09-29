package com.wethinkcode.bloodbroker.config;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    
    private final BloodInventoryRepository repository;

    public DataSeeder(BloodInventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        log.info("[SEEDER] Attempting to inject mock blood inventory data into AWS DynamoDB...");
        
        try {
            // Create 50 mock records
            Random random = new Random();
            String[] bloodTypes = {"O-Negative", "O-Positive", "A-Negative", "A-Positive", "B-Negative", "B-Positive", "AB-Negative", "AB-Positive"};

            for (int i = 1; i <= 50; i++) {
                String bankName = "Regional Bank #" + i;
                String bloodType = bloodTypes[random.nextInt(bloodTypes.length)];
                int units = random.nextInt(100); // 0 to 99 units
                
                BloodInventoryStatus status = new BloodInventoryStatus(bankName, bloodType, units, (units == 0));
                repository.save(status);
            }

            log.info("[SEEDER] Successfully injected 50 mock records into DynamoDB!");
            
        } catch (Exception e) {
            log.warn("[SEEDER] Skipping data injection. Could not connect to AWS or table 'BloodInventory' missing. Reason: {}", e.getMessage());
        }
    }
}
