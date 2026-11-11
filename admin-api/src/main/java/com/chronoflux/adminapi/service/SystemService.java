package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.model.EventStatus;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import com.chronoflux.adminapi.repository.UserRepository;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import org.springframework.stereotype.Service;

import com.chronoflux.adminapi.dto.SystemStatusDTO;

import java.util.List;

@Service
public class SystemService {
    private final LiveEventRepository liveEventRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final UserRepository userRepository;

    public SystemService(LiveEventRepository liveEventRepository,
                            WebhookDeliveryRepository webhookDeliveryRepository,
                            UserRepository userRepository) {
        this.liveEventRepository = liveEventRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.userRepository = userRepository;
    }

    public SystemStatusDTO getSystemStatus() {
        long activeEvents = liveEventRepository.countByStatus(EventStatus.ACTIVE);
        // "PENDING" was a string mismatch bug; worker records "ATTEMPTING" and "RETRYING"
        long pendingWebhooks = webhookDeliveryRepository.countByStatusIn(List.of("ATTEMPTING", "RETRYING"));
        long totalUsers = userRepository.count();

        return new SystemStatusDTO(activeEvents, pendingWebhooks, totalUsers, "HEALTHY");
    }
}
