package com.example.webhook_worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
     * Sends the payload to the target URL. BLOCKS until the response is received.
     * This ensures the RabbitMQ consumer thread does not ACK until delivery completes.
     */
    public void sendPayload(String targetUrl, String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonNode)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // BLOCKING — thread pauses until HTTP response
        } catch (Exception e) {
            throw new RuntimeException("Webhook delivery failed to " + targetUrl, e);
        }
    }
}
