package com.chronoflux.adminapi.mapper;

import com.chronoflux.adminapi.dto.WebhookCreateDTO;
import com.chronoflux.adminapi.dto.WebhookDTO;
import com.chronoflux.adminapi.dto.WebhookDeliveryDTO;
import com.chronoflux.adminapi.model.Webhook;
import com.chronoflux.adminapi.model.WebhookDelivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WebhookMapper {

    @Mapping(source = "schema.id", target = "schemaId")
    WebhookDTO toDTO(Webhook webhook);

    List<WebhookDTO> toDTOList(List<Webhook> webhooks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schema", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Webhook toEntity(WebhookCreateDTO dto);

    WebhookDeliveryDTO toDeliveryDTO(WebhookDelivery delivery);
    List<WebhookDeliveryDTO> toDeliveryDTOList(List<WebhookDelivery> deliveries);
}
