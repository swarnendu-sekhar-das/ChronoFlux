package com.chronoflux.mockserver.controller;

import com.chronoflux.mockserver.dto.WebhookPayloadDto;
import com.chronoflux.mockserver.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/game-server")
    public ResponseEntity<Void> receiveWebhook(@RequestBody WebhookPayloadDto payload) {
        webhookService.processWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
