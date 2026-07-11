package com.chronoflux.adminapi.mapper;

import com.chronoflux.adminapi.dto.EventSchemaCreateDTO;
import com.chronoflux.adminapi.dto.EventSchemaDTO;
import com.chronoflux.adminapi.model.EventSchema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventSchemaMapper {

    EventSchemaDTO toDTO(EventSchema eventSchema);
    
    List<EventSchemaDTO> toDTOList(List<EventSchema> eventSchemas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EventSchema toEntity(EventSchemaCreateDTO createDTO);
}
