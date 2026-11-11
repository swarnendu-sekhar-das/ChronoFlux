package com.example.webhook_worker.exception;

public class NonRetryableWebhookException extends WebhookDeliveryException {
    public NonRetryableWebhookException(int statusCode, String message) {
        super(statusCode, message, false);
    }
}

