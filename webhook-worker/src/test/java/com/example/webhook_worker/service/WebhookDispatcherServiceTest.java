package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.exception.NonRetryableWebhookException;
import com.example.webhook_worker.exception.RetryableWebhookException;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDispatcherServiceTest {

    @Mock
    private WebhookHttpClient httpClient;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @InjectMocks
    private WebhookDispatcherService dispatcherService;

    private WebhookDelivery delivery;

    @BeforeEach
    void setUp() {
        delivery = new WebhookDelivery();
        delivery.setId(UUID.randomUUID());
        delivery.setTargetUrl("https://game-server.example.com/webhook");
        delivery.setPayload("{\"eventId\":\"123\"}");
        delivery.setStatus("ATTEMPTING");
    }

    @Test
    void deliverWithRetry_Success_RecordsStatusSuccessAndCode() {
        when(httpClient.sendPayload(delivery.getTargetUrl(), delivery.getPayload(), delivery.getId().toString()))
                .thenReturn(200);

        dispatcherService.deliverWithRetry(delivery);

        assertEquals("SUCCESS", delivery.getStatus());
        assertEquals(200, delivery.getResponseCode());
        verify(deliveryRepository, times(1)).save(delivery);
    }

    @Test
    void markAsFailed_NonRetryableException_SetsPermanentlyFailed() {
        NonRetryableWebhookException exception = new NonRetryableWebhookException(404, "Endpoint not found");

        dispatcherService.markAsFailed(delivery, exception);

        assertEquals("PERMANENTLY_FAILED", delivery.getStatus());
        assertEquals(404, delivery.getResponseCode());
        assertEquals("Endpoint not found", delivery.getFailureReason());
        verify(deliveryRepository, times(1)).save(delivery);
    }

    @Test
    void markAsFailed_RetryableException_SetsFailedForReconciliation() {
        RetryableWebhookException exception = new RetryableWebhookException(503, "Service Unavailable");

        dispatcherService.markAsFailed(delivery, exception);

        assertEquals("FAILED", delivery.getStatus());
        assertEquals(503, delivery.getResponseCode());
        assertEquals("Service Unavailable", delivery.getFailureReason());
        verify(deliveryRepository, times(1)).save(delivery);
    }
}

