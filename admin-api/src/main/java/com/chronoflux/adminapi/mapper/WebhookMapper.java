package com.chronoflux.adminapi.mapper;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookDeliveryDTO;
import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.model.WebhookDelivery;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WebhookMapper {
    WebhookDTO toDTO(Webhook webhook);
    List<WebhookDTO> toDTOList(List<Webhook> webhooks);
    Webhook toEntity(WebhookCreateDTO dto);

    WebhookDeliveryDTO toDeliveryDTO(WebhookDelivery delivery);
    List<WebhookDeliveryDTO> toDeliveryDTOList(List<WebhookDelivery> deliveries);
}
