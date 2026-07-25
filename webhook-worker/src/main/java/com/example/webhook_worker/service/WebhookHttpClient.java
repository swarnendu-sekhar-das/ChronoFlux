package com.example.webhook_worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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

    public Mono<Void> sendPayload(String targetUrl, String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            return webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonNode)
                    .retrieve()
                    .toBodilessEntity()
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
