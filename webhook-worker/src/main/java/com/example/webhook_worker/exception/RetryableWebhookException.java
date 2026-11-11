package com.example.webhook_worker.exception;

public class RetryableWebhookException extends WebhookDeliveryException {
    public RetryableWebhookException(int statusCode, String message) {
        super(statusCode, message, true);
    }

    public RetryableWebhookException(int statusCode, String message, Throwable cause) {
        super(statusCode, message, true, cause);
    }
}

