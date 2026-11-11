package com.example.webhook_worker.service;

import com.example.webhook_worker.exception.WebhookDeliveryException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebhookHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(WebhookHttpClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookHttpClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the payload to the target URL.
     * Blocks until response is received to tie RabbitMQ acknowledgment to delivery outcome.
     * 
     * @param targetUrl destination webhook endpoint
     * @param payload JSON envelope payload
     * @param deliveryId unique delivery UUID for downstream idempotency tracking
     * @return HTTP status code of the response (e.g., 200, 201, 204)
     */
    public int sendPayload(String targetUrl, String payload, String deliveryId) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            ResponseEntity<Void> response = webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Delivery-Id", deliveryId != null ? deliveryId : "")
                    .bodyValue(jsonNode)
                    .retrieve()
                    // 4xx Client Errors: endpoint not found or bad request -> non-retryable
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("No response body")
                            .flatMap(body -> {
                                int code = clientResponse.statusCode().value();
                                logger.warn("Target {} returned 4xx client error ({}): {}", targetUrl, code, body);
                                return Mono.error(new com.example.webhook_worker.exception.NonRetryableWebhookException(
                                    code,
                                    "HTTP " + code + " client error: " + body
                                ));
                            })
                    )
                    // 5xx Server Errors: target crash, overload -> retryable
                    .onStatus(HttpStatusCode::is5xxServerError, serverResponse ->
                        serverResponse.bodyToMono(String.class)
                            .defaultIfEmpty("No response body")
                            .flatMap(body -> {
                                int code = serverResponse.statusCode().value();
                                logger.warn("Target {} returned 5xx server error ({}): {}", targetUrl, code, body);
                                return Mono.error(new com.example.webhook_worker.exception.RetryableWebhookException(
                                    code,
                                    "HTTP " + code + " server error: " + body
                                ));
                            })
                    )
                    .toBodilessEntity()
                    .block(); // Blocking call for synchronous dispatch on consumer worker thread

            return response != null ? response.getStatusCode().value() : 200;
        } catch (WebhookDeliveryException e) {
            // Propagate domain exception directly
            throw e;
        } catch (Exception e) {
            // Network failures, connection refused, DNS resolution, socket timeouts -> retryable
            logger.error("Network error delivering webhook to {}: {}", targetUrl, e.getMessage());
            throw new com.example.webhook_worker.exception.RetryableWebhookException(500, "Network error: " + e.getMessage(), e);
        }
    }
}
