package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookDeliveryDTO;
import com.chronoflux.adminapi.dto.WebhookToggleDTO;
import com.chronoflux.adminapi.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks")
@CrossOrigin("*") // Dev only — in production, restrict this to the admin frontend's origin.
@Tag(name = "Webhooks", description = "Endpoints for managing Webhooks and Deliveries")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "Get all webhooks", description = "Retrieves all registered webhooks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved webhooks")
    @GetMapping
    public ResponseEntity<Page<WebhookDTO>> getWebhooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(webhookService.getWebhooks(PageRequest.of(page, size)));
    }

    @Operation(summary = "Create a webhook", description = "Registers a new webhook for an event schema")
    @ApiResponse(responseCode = "200", description = "Webhook successfully created")
    @PostMapping
    public ResponseEntity<WebhookDTO> createWebhook(@Valid @RequestBody WebhookCreateDTO dto) {
        return ResponseEntity.ok(webhookService.createWebhook(dto));
    }

    @Operation(summary = "Toggle webhook", description = "Enables or disables a webhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully toggled webhook"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @PutMapping("/{id}/toggle")
    public ResponseEntity<WebhookDTO> toggleWebhook(@PathVariable UUID id, @Valid @RequestBody WebhookToggleDTO dto) {
        return webhookService.toggleWebhook(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get webhook deliveries", description = "Retrieves the history of webhook deliveries (including failed delivery capture logs)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved deliveries")
    @GetMapping("/deliveries")
    public ResponseEntity<Page<WebhookDeliveryDTO>> getWebhookDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(webhookService.getWebhookDeliveries(PageRequest.of(page, size)));
    }
}
