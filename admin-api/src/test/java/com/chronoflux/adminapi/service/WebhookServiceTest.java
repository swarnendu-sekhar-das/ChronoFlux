package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookToggleDTO;
import com.chronoflux.adminapi.mapper.WebhookMapper;
import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import com.chronoflux.adminapi.repository.WebhookDeliveryRepository;
import com.chronoflux.adminapi.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private EventSchemaRepository schemaRepository;

    @Mock
    private WebhookMapper webhookMapper;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(webhookRepository, deliveryRepository, schemaRepository, webhookMapper);
    }

    @Test
    void createWebhook_SchemaNotFound_ThrowsIllegalArgumentException() {
        UUID schemaId = UUID.randomUUID();
        WebhookCreateDTO dto = new WebhookCreateDTO();
        dto.setSchemaId(schemaId);
        dto.setTargetUrl("https://example.com/webhook");

        when(schemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> webhookService.createWebhook(dto));
        verify(webhookRepository, never()).save(any());
    }

    @Test
    void createWebhook_ValidSchema_AssociatesSchemaAndSaves() {
        UUID schemaId = UUID.randomUUID();
        WebhookCreateDTO dto = new WebhookCreateDTO();
        dto.setSchemaId(schemaId);
        dto.setTargetUrl("https://example.com/webhook");

        EventSchema schema = new EventSchema();
        schema.setId(schemaId);
        schema.setName("RaidEvent");

        Webhook webhookEntity = new Webhook();
        webhookEntity.setTargetUrl(dto.getTargetUrl());

        WebhookDTO webhookDTO = new WebhookDTO();
        webhookDTO.setSchemaId(schemaId);
        webhookDTO.setTargetUrl(dto.getTargetUrl());

        when(schemaRepository.findById(schemaId)).thenReturn(Optional.of(schema));
        when(webhookMapper.toEntity(dto)).thenReturn(webhookEntity);
        when(webhookRepository.save(webhookEntity)).thenReturn(webhookEntity);
        when(webhookMapper.toDTO(webhookEntity)).thenReturn(webhookDTO);

        WebhookDTO result = webhookService.createWebhook(dto);

        assertNotNull(result);
        assertEquals(schema, webhookEntity.getSchema());
        assertTrue(webhookEntity.getIsActive());
        verify(webhookRepository, times(1)).save(webhookEntity);
    }

    @Test
    void toggleWebhook_ExistingWebhook_UpdatesIsActive() {
        UUID webhookId = UUID.randomUUID();
        Webhook webhook = new Webhook();
        webhook.setId(webhookId);
        webhook.setIsActive(true);

        WebhookToggleDTO toggleDTO = new WebhookToggleDTO();
        toggleDTO.setIsActive(false);

        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(webhook));
        when(webhookRepository.save(webhook)).thenReturn(webhook);
        when(webhookMapper.toDTO(webhook)).thenReturn(new WebhookDTO());

        Optional<WebhookDTO> result = webhookService.toggleWebhook(webhookId, toggleDTO);

        assertTrue(result.isPresent());
        assertFalse(webhook.getIsActive());
        verify(webhookRepository, times(1)).save(webhook);
    }
}
