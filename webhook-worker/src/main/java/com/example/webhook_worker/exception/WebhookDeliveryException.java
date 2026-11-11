package com.example.webhook_worker.exception;

/**
 * Custom domain exception for webhook dispatch failures.
 * Distinguishes between transient failures (5xx, timeouts) that should be retried,
 * and permanent failures (4xx client errors) that should fail-fast.
 */
public class WebhookDeliveryException extends RuntimeException {

    private final int statusCode;
    private final boolean retryable;

    public WebhookDeliveryException(int statusCode, String message, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public WebhookDeliveryException(int statusCode, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

