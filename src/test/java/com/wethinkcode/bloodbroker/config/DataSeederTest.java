package com.wethinkcode.bloodbroker.config;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private BloodInventoryRepository repository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void testDataSeederSaves50Records() throws Exception {
        // Run the seeder
        dataSeeder.run();

        // Verify repository.save was called exactly 50 times
        ArgumentCaptor<BloodInventoryStatus> captor = ArgumentCaptor.forClass(BloodInventoryStatus.class);
        verify(repository, times(50)).save(captor.capture());

        // Validate the structure of one of the saved records
        BloodInventoryStatus savedStatus = captor.getAllValues().get(0);
        assertNotNull(savedStatus.getBankName());
        assertTrue(savedStatus.getBankName().startsWith("Regional Bank #"));
        assertNotNull(savedStatus.getBloodType());
        assertTrue(savedStatus.getUnitsAvailable() >= 0 && savedStatus.getUnitsAvailable() < 100);
    }

    @Test
    void testDataSeederHandlesExceptionGracefully() throws Exception {
        // Make the repository throw an exception
        doThrow(new RuntimeException("DynamoDB unreachable")).when(repository).save(any(BloodInventoryStatus.class));

        // The run method should catch the exception and not throw it further
        assertDoesNotThrow(() -> dataSeeder.run());
    }
}
