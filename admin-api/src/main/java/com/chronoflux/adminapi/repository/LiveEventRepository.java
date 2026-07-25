package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    long countByStatus(com.chronoflux.adminapi.model.EventStatus status);
    boolean existsBySchemaId(UUID schemaId);
    org.springframework.data.domain.Page<LiveEvent> findByStatusNot(com.chronoflux.adminapi.model.EventStatus status, org.springframework.data.domain.Pageable pageable);
}
