package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.EventSchemaCreateDTO;
import com.chronoflux.adminapi.dto.EventSchemaDTO;
import com.chronoflux.adminapi.mapper.EventSchemaMapper;
import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<EventSchemaDTO> getAllSchemas(boolean includeArchived) {
        if (includeArchived) {
            return schemaMapper.toDTOList(schemaRepository.findAll());
        }
        return schemaMapper.toDTOList(schemaRepository.findByArchivedFalse());
    }

    public EventSchemaDTO createSchema(EventSchemaCreateDTO schemaCreateDTO) {
        EventSchema schema = schemaMapper.toEntity(schemaCreateDTO);
        EventSchema savedSchema = schemaRepository.save(schema);
        return schemaMapper.toDTO(savedSchema);
    }

    public Optional<EventSchemaDTO> getSchemaById(UUID id) {
        return schemaRepository.findById(id).map(schemaMapper::toDTO);
    }

    @Transactional
    public Optional<EventSchemaDTO> archiveSchema(UUID id) {
        return schemaRepository.findById(id).map(schema -> {
            schema.setArchived(true);
            EventSchema archivedSchema = schemaRepository.save(schema);
            return schemaMapper.toDTO(archivedSchema);
        });
    }
}
