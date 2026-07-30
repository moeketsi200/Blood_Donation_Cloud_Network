package com.wethinkcode.bloodbroker.config;

import com.wethinkcode.bloodbroker.domain.HospitalRequest;
import com.wethinkcode.bloodbroker.transformer.PayloadEnricher;
import com.wethinkcode.bloodbroker.transformer.XmlToCanonicalTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SftpInboundConfigTest {

    @Test
    void testPollQueueNoQueueUrl() {
        XmlToCanonicalTransformer transformer = mock(XmlToCanonicalTransformer.class);
        PayloadEnricher enricher = mock(PayloadEnricher.class);
        SftpInboundConfig config = new SftpInboundConfig(transformer, enricher);

        assertDoesNotThrow(config::pollQueue);
    }

    @Test
    void testPollQueueSuccessfulMessageProcessing() {
        XmlToCanonicalTransformer transformer = new XmlToCanonicalTransformer();
        PayloadEnricher enricher = new PayloadEnricher();
        SftpInboundConfig config = new SftpInboundConfig(transformer, enricher);

        String queueUrl = "https://sqs.eu-north-1.amazonaws.com/12345/test-queue";
        ReflectionTestUtils.setField(config, "queueUrl", queueUrl);

        SqsClient mockSqsClient = mock(SqsClient.class);
        ReflectionTestUtils.setField(config, "sqsClient", mockSqsClient);

        String xmlBody = "<hospitalRequest><bloodType>O-Negative</bloodType><unitsRequired>4</unitsRequired><hospitalId>HOSP-100</hospitalId></hospitalRequest>";
        Message message = Message.builder()
                .messageId("msg-1")
                .body(xmlBody)
                .receiptHandle("receipt-handle-1")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        config.pollQueue();

        verify(mockSqsClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testPollQueueHandlesProcessingError() {
        XmlToCanonicalTransformer transformer = mock(XmlToCanonicalTransformer.class);
        PayloadEnricher enricher = mock(PayloadEnricher.class);
        SftpInboundConfig config = new SftpInboundConfig(transformer, enricher);

        String queueUrl = "https://sqs.eu-north-1.amazonaws.com/12345/test-queue";
        ReflectionTestUtils.setField(config, "queueUrl", queueUrl);

        SqsClient mockSqsClient = mock(SqsClient.class);
        ReflectionTestUtils.setField(config, "sqsClient", mockSqsClient);

        Message message = Message.builder()
                .messageId("msg-bad")
                .body("invalid xml")
                .receiptHandle("receipt-handle-bad")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
        when(transformer.transform(any())).thenThrow(new RuntimeException("XML parse error"));

        assertDoesNotThrow(config::pollQueue);
        verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
