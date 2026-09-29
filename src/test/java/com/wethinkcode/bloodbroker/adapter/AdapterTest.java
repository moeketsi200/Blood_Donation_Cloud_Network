package com.wethinkcode.bloodbroker.adapter;

import com.wethinkcode.bloodbroker.domain.BloodInventoryStatus;
import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import com.wethinkcode.bloodbroker.repository.BloodInventoryRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        BloodInventoryRepository mockRepo = mock(BloodInventoryRepository.class);
        BloodInventoryStatus mockStatus = new BloodInventoryStatus("Modern Bank B", "A-Positive", 3, false);
        when(mockRepo.getStatus("Modern Bank B", "A-Positive")).thenReturn(mockStatus);
        
        ModernBankRestAdapter adapter = new ModernBankRestAdapter(mockRepo);
        HospitalRequest request = new HospitalRequest();
        request.setBloodType("A-Positive");

        BloodInventoryStatus status = adapter.queryInventory(request);

        assertNotNull(status, "Adapter should return a mock status object");
        assertNotNull(status.getBankName(), "Bank name should be populated");
    }

    @Test
    void testTwilioNotificationAdapterWithoutTopicArn() {
        TwilioNotificationAdapter adapter = new TwilioNotificationAdapter();
        assertDoesNotThrow(() -> adapter.triggerDonorAlert("O-Negative"));
    }

    @Test
    void testTwilioNotificationAdapterWithSnsSuccess() {
        TwilioNotificationAdapter adapter = new TwilioNotificationAdapter();
        ReflectionTestUtils.setField(adapter, "topicArn", "arn:aws:sns:eu-north-1:123456789:test-topic");

        SnsClient mockSnsClient = mock(SnsClient.class);
        PublishResponse mockResponse = PublishResponse.builder().messageId("msg-123").build();
        when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResponse);

        adapter.setSnsClient(mockSnsClient);

        assertDoesNotThrow(() -> adapter.triggerDonorAlert("O-Negative"));
        verify(mockSnsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void testTwilioNotificationAdapterWithSnsError() {
        TwilioNotificationAdapter adapter = new TwilioNotificationAdapter();
        ReflectionTestUtils.setField(adapter, "topicArn", "arn:aws:sns:eu-north-1:123456789:test-topic");

        SnsClient mockSnsClient = mock(SnsClient.class);
        when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("AWS Error"));

        adapter.setSnsClient(mockSnsClient);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adapter.triggerDonorAlert("AB-Negative"));
        assertTrue(ex.getMessage().contains("Failed to send emergency SNS alert"));
    }
}
