package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookDeliveryDTO;
import com.chronoflux.adminapi.dto.WebhookToggleDTO;
import com.chronoflux.adminapi.mapper.WebhookMapper;
import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import com.chronoflux.adminapi.repository.WebhookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {
    
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final EventSchemaRepository schemaRepository;
    private final WebhookMapper webhookMapper;

    public WebhookService(WebhookRepository webhookRepository, 
                          WebhookDeliveryRepository webhookDeliveryRepository, 
                          EventSchemaRepository schemaRepository,
                          WebhookMapper webhookMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.schemaRepository = schemaRepository;
        this.webhookMapper = webhookMapper;
    }

    public Page<WebhookDTO> getWebhooks(Pageable pageable) {
        return webhookRepository.findAll(pageable).map(webhookMapper::toDTO);
    }

    @Transactional
    public WebhookDTO createWebhook(WebhookCreateDTO dto) {
        // Enforce JPA relationship: validate schema exists and associate managed EventSchema
        EventSchema schema = schemaRepository.findById(dto.getSchemaId())
                .orElseThrow(() -> new IllegalArgumentException("Event schema not found with ID: " + dto.getSchemaId()));

        Webhook webhook = webhookMapper.toEntity(dto);
        webhook.setSchema(schema);
        webhook.setIsActive(true);
        return webhookMapper.toDTO(webhookRepository.save(webhook));
    }

    @Transactional
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
