package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.model.EventStatus;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import com.chronoflux.adminapi.repository.UserRepository;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final LiveEventRepository liveEventRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final UserRepository userRepository;

    public SystemController(LiveEventRepository liveEventRepository,
                            WebhookDeliveryRepository webhookDeliveryRepository,
                            UserRepository userRepository) {
        this.liveEventRepository = liveEventRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        long activeEvents = liveEventRepository.countByStatus(EventStatus.ACTIVE);
        long pendingWebhooks = webhookDeliveryRepository.countByStatus("PENDING");
        long totalUsers = userRepository.count();

        Map<String, Object> status = new HashMap<>();
        status.put("activeEvents", activeEvents);
        status.put("pendingWebhooks", pendingWebhooks);
        status.put("totalUsers", totalUsers);
        status.put("status", "HEALTHY");

        return ResponseEntity.ok(status);
    }
}
