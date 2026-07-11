package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.EventSchemaCreateDTO;
import com.chronoflux.adminapi.dto.EventSchemaDTO;
import com.chronoflux.adminapi.mapper.EventSchemaMapper;
import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventSchemaService {

    private final EventSchemaRepository schemaRepository;
    private final EventSchemaMapper schemaMapper;

    public EventSchemaService(EventSchemaRepository schemaRepository, EventSchemaMapper schemaMapper) {
        this.schemaRepository = schemaRepository;
        this.schemaMapper = schemaMapper;
    }

    public List<EventSchemaDTO> getAllSchemas() {
        return schemaMapper.toDTOList(schemaRepository.findAll());
    }

    public EventSchemaDTO createSchema(EventSchemaCreateDTO schemaCreateDTO) {
        EventSchema schema = schemaMapper.toEntity(schemaCreateDTO);
        EventSchema savedSchema = schemaRepository.save(schema);
        return schemaMapper.toDTO(savedSchema);
    }

    public Optional<EventSchemaDTO> getSchemaById(UUID id) {
        return schemaRepository.findById(id).map(schemaMapper::toDTO);
    }
}
