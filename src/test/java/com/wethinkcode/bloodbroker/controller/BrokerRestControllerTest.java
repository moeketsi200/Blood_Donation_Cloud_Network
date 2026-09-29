package com.wethinkcode.bloodbroker.controller;

import com.wethinkcode.bloodbroker.adapter.LegacyBankSoapAdapter;
import com.wethinkcode.bloodbroker.adapter.ModernBankRestAdapter;
import com.wethinkcode.bloodbroker.adapter.TwilioNotificationAdapter;
import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;
import com.wethinkcode.bloodbroker.router.BloodBankRouter;
import com.wethinkcode.bloodbroker.transformer.PayloadEnricher;
import com.wethinkcode.bloodbroker.transformer.XmlToCanonicalTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BrokerRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private XmlToCanonicalTransformer transformer;
    @MockBean private PayloadEnricher enricher;
    @MockBean private LegacyBankSoapAdapter soapAdapter;
    @MockBean private ModernBankRestAdapter restAdapter;
    @MockBean private BloodBankRouter router;
    @MockBean private TwilioNotificationAdapter notificationAdapter;
    @MockBean private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BrokerRestController controller;

    @BeforeEach
    void setUp() {
        clearInvocations(inventoryRepository);
        ReflectionTestUtils.setField(controller, "queueUrl", "https://sqs.eu-north-1.amazonaws.com/123/queue");
        ReflectionTestUtils.setField(controller, "awsRegion", "eu-north-1");
    }

    @Test
    void testProcessXmlPayload() throws Exception {
        String rawXml = "<request></request>";
        HospitalRequest req = new HospitalRequest("O-Negative", 5, "HOSP-1");
        HospitalRequest enriched = new HospitalRequest("O-Negative", 5, "HOSP-1");
        enriched.setTimestamp("2023-10-01T12:00:00Z");

        BloodInventoryStatus bankA = new BloodInventoryStatus("Bank A", "O-Negative", 2, false);
        BloodInventoryStatus bankB = new BloodInventoryStatus("Bank B", "O-Negative", 3, false);
        BloodInventoryStatus aggregated = new BloodInventoryStatus("Aggregated", "O-Negative", 5, false);

        when(transformer.transform(anyString())).thenReturn(req);
        when(enricher.enrich(any(HospitalRequest.class))).thenReturn(enriched);
        when(soapAdapter.checkStock(any(HospitalRequest.class))).thenReturn(bankA);
        when(restAdapter.queryInventory(any(HospitalRequest.class))).thenReturn(bankB);
        when(router.aggregate(anyList())).thenReturn(aggregated);

        mockMvc.perform(post("/api/broker/process-xml")
                .contentType(MediaType.APPLICATION_XML)
                .content(rawXml))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.notification_status").value("STOCK_AVAILABLE_NO_ALERT"));

        verify(inventoryRepository, times(1)).save(aggregated);
    }

    @Test
    void testReserveInventory() throws Exception {
        HospitalRequest req = new HospitalRequest("A-Positive", 10, "HOSP-2");
        when(transformer.transform(anyString())).thenReturn(req);
        when(inventoryRepository.reserveInventory("A-Positive", 10)).thenReturn(10);

        mockMvc.perform(post("/api/broker/reserve")
                .contentType(MediaType.APPLICATION_XML)
                .content("<test></test>"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.reserved_units").value(10));
    }

    @Test
    void testSupplyInventory_ExistingBank() throws Exception {
        BloodInventoryStatus existing = new BloodInventoryStatus("Bank A", "B-Negative", 5, false);
        when(inventoryRepository.getStatus("Bank A", "B-Negative")).thenReturn(existing);

        mockMvc.perform(post("/api/broker/supply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bankName\":\"Bank A\", \"bloodType\":\"B-Negative\", \"units\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(10, existing.getUnitsAvailable());
        verify(inventoryRepository, times(1)).save(existing);
    }

    @Test
    void testSupplyInventory_NewBank() throws Exception {
        when(inventoryRepository.getStatus("New Bank", "AB-Positive")).thenReturn(null);

        mockMvc.perform(post("/api/broker/supply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bankName\":\"New Bank\", \"bloodType\":\"AB-Positive\", \"units\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(inventoryRepository, times(1)).save(any(BloodInventoryStatus.class));
    }

    @Test
    void testGetDatabaseInventory() throws Exception {
        BloodInventoryStatus stat1 = new BloodInventoryStatus("Bank A", "O-Negative", 5, false);
        when(inventoryRepository.getAllInventory()).thenReturn(Arrays.asList(stat1));

        mockMvc.perform(get("/api/broker/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bankName").value("Bank A"));
    }

    @Test
    void testGetTelemetry() throws Exception {
        mockMvc.perform(get("/api/broker/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broker_name").value("Blood Integration Broker"))
                .andExpect(jsonPath("$.aws_region").value("eu-north-1"))
                .andExpect(jsonPath("$.uptime_status").value("HEALTHY_ONLINE"));
    }

    @Test
    void testPublishToSqs_QueueUrlNotConfigured() throws Exception {
        ReflectionTestUtils.setField(controller, "queueUrl", "");

        mockMvc.perform(post("/api/broker/sqs/publish")
                .contentType(MediaType.APPLICATION_XML)
                .content("<request>123</request>"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").value("AWS SQS Queue URL is not configured."));
    }
}
