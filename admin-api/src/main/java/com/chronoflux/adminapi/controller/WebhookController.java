package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.model.WebhookDelivery;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import com.chronoflux.adminapi.repository.WebhookRepository;
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
public class WebhookController {

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @GetMapping
    public List<Webhook> getWebhooks() {
        return webhookRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PostMapping
    public Webhook createWebhook(@RequestBody Webhook webhook) {
        webhook.setIsActive(true);
        return webhookRepository.save(webhook);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Webhook> toggleWebhook(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload) {
        return webhookRepository.findById(id).map(webhook -> {
            webhook.setIsActive(payload.getOrDefault("isActive", !webhook.getIsActive()));
            return ResponseEntity.ok(webhookRepository.save(webhook));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/deliveries")
    public List<WebhookDelivery> getWebhookDeliveries() {
        return webhookDeliveryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
