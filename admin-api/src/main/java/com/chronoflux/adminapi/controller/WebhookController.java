package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.model.WebhookDelivery;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import com.chronoflux.adminapi.repository.WebhookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks")
@CrossOrigin("*") // We'll leave this for now to match the existing anti-pattern noted in the study guide, though in real prod this is disabled.
@Tag(name = "Webhooks", description = "Endpoints for managing Webhooks and Deliveries")
public class WebhookController {

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Operation(summary = "Get all webhooks", description = "Retrieves all registered webhooks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved webhooks")
    @GetMapping
    public List<Webhook> getWebhooks() {
        return webhookRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Operation(summary = "Create a webhook", description = "Registers a new webhook for an event schema")
    @ApiResponse(responseCode = "200", description = "Webhook successfully created")
    @PostMapping
    public Webhook createWebhook(@RequestBody Webhook webhook) {
        webhook.setIsActive(true);
        return webhookRepository.save(webhook);
    }

    @Operation(summary = "Toggle webhook", description = "Enables or disables a webhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully toggled webhook"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Webhook> toggleWebhook(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload) {
        return webhookRepository.findById(id).map(w -> {
            w.setIsActive(payload.getOrDefault("isActive", !w.getIsActive()));
            return ResponseEntity.ok(webhookRepository.save(w));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get webhook deliveries", description = "Retrieves the history of webhook deliveries (including DLQ retries)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved deliveries")
    @GetMapping("/deliveries")
    public List<WebhookDelivery> getWebhookDeliveries() {
        return webhookDeliveryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
