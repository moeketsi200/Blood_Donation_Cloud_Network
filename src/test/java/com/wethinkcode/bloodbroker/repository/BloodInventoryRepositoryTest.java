package com.wethinkcode.bloodbroker.repository;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloodInventoryRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<BloodInventoryStatus> inventoryTable;

    private BloodInventoryRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("BloodInventory"), any(TableSchema.class))).thenReturn(inventoryTable);
        repository = new BloodInventoryRepository(enhancedClient);
    }

    @Test
    void testSave() {
        BloodInventoryStatus status = new BloodInventoryStatus("Bank A", "O-Negative", 10, false);
        repository.save(status);
        verify(inventoryTable, times(1)).putItem(status);
    }

    @Test
    void testGetStatus() {
        BloodInventoryStatus mockStatus = new BloodInventoryStatus("Bank A", "O-Negative", 10, false);
        
        // Mock the getItem call passing a consumer
        when(inventoryTable.getItem(any(Consumer.class))).thenReturn(mockStatus);

        BloodInventoryStatus result = repository.getStatus("Bank A", "O-Negative");
        assertNotNull(result);
        assertEquals("Bank A", result.getBankName());
        assertEquals("O-Negative", result.getBloodType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAllInventory() {
        BloodInventoryStatus status1 = new BloodInventoryStatus("Bank A", "O-Negative", 10, false);
        BloodInventoryStatus status2 = new BloodInventoryStatus("Bank B", "A-Positive", 5, false);
        
        PageIterable<BloodInventoryStatus> pageIterable = mock(PageIterable.class);
        SdkIterable<BloodInventoryStatus> itemsIterable = mock(SdkIterable.class);
        
        when(inventoryTable.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(itemsIterable);
        when(itemsIterable.stream()).thenReturn(Stream.of(status1, status2));

        List<BloodInventoryStatus> result = repository.getAllInventory();
        assertEquals(2, result.size());
        assertEquals("Bank A", result.get(0).getBankName());
        assertEquals("Bank B", result.get(1).getBankName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReserveInventory_FullyFulfillable() {
        BloodInventoryStatus bankA = new BloodInventoryStatus("Bank A", "O-Negative", 10, false);
        
        PageIterable<BloodInventoryStatus> pageIterable = mock(PageIterable.class);
        SdkIterable<BloodInventoryStatus> itemsIterable = mock(SdkIterable.class);
        when(inventoryTable.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(itemsIterable);
        when(itemsIterable.stream()).thenReturn(Stream.of(bankA));

        int reserved = repository.reserveInventory("O-Negative", 5);
        
        assertEquals(5, reserved);
        verify(inventoryTable, times(1)).putItem(bankA);
        assertEquals(5, bankA.getUnitsAvailable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReserveInventory_PartiallyFulfillableFromMultipleBanks() {
        BloodInventoryStatus bankA = new BloodInventoryStatus("Bank A", "O-Negative", 3, false);
        BloodInventoryStatus bankB = new BloodInventoryStatus("Bank B", "O-Negative", 4, false);
        BloodInventoryStatus bankC = new BloodInventoryStatus("Bank C", "O-Positive", 10, false); // different type
        
        PageIterable<BloodInventoryStatus> pageIterable = mock(PageIterable.class);
        SdkIterable<BloodInventoryStatus> itemsIterable = mock(SdkIterable.class);
        when(inventoryTable.scan()).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(itemsIterable);
        when(itemsIterable.stream()).thenReturn(Stream.of(bankA, bankB, bankC));

        int reserved = repository.reserveInventory("O-Negative", 5);
        
        assertEquals(5, reserved);
        // Bank A is drained (3 -> 0)
        assertEquals(0, bankA.getUnitsAvailable());
        verify(inventoryTable, times(1)).putItem(bankA);
        
        // Bank B fulfills the remaining 2 units (4 -> 2)
        assertEquals(2, bankB.getUnitsAvailable());
        verify(inventoryTable, times(1)).putItem(bankB);
        
        // Bank C should not be touched
        verify(inventoryTable, never()).putItem(bankC);
    }
}
