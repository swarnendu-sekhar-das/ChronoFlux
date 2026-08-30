package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookDeliveryDTO;
import com.chronoflux.adminapi.dto.WebhookToggleDTO;
import com.chronoflux.adminapi.mapper.WebhookMapper;
import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import com.chronoflux.adminapi.repository.WebhookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {
    
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookMapper webhookMapper;

    public WebhookService(WebhookRepository webhookRepository, WebhookDeliveryRepository webhookDeliveryRepository, WebhookMapper webhookMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookMapper = webhookMapper;
    }

    public Page<WebhookDTO> getWebhooks(Pageable pageable) {
        return webhookRepository.findAll(pageable).map(webhookMapper::toDTO);
    }

    public WebhookDTO createWebhook(WebhookCreateDTO dto) {
        Webhook webhook = webhookMapper.toEntity(dto);
        webhook.setIsActive(true);
        return webhookMapper.toDTO(webhookRepository.save(webhook));
    }

    public Optional<WebhookDTO> toggleWebhook(UUID id, WebhookToggleDTO dto) {
        return webhookRepository.findById(id).map(w -> {
            w.setIsActive(dto.getIsActive());
            return webhookMapper.toDTO(webhookRepository.save(w));
        });
    }

    public Page<WebhookDeliveryDTO> getWebhookDeliveries(Pageable pageable) {
        return webhookDeliveryRepository.findAll(pageable).map(webhookMapper::toDeliveryDTO);
    }
}
