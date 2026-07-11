package com.example.edge_api.mapper;

import com.example.edge_api.domain.LiveEvent;
import com.example.edge_api.dto.LiveEventResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LiveEventMapper {
    LiveEventResponseDTO toDTO(LiveEvent liveEvent);
    List<LiveEventResponseDTO> toDTOList(List<LiveEvent> liveEvents);
}
