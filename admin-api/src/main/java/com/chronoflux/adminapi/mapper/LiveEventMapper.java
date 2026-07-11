package com.chronoflux.adminapi.mapper;

import com.chronoflux.adminapi.dto.LiveEventCreateDTO;
import com.chronoflux.adminapi.dto.LiveEventDTO;
import com.chronoflux.adminapi.model.LiveEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {EventSchemaMapper.class})
public interface LiveEventMapper {

    LiveEventDTO toDTO(LiveEvent liveEvent);
    
    List<LiveEventDTO> toDTOList(List<LiveEvent> liveEvents);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LiveEvent toEntity(LiveEventCreateDTO createDTO);
}
