package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.chronoflux.adminapi.model.EventStatus;

import java.util.UUID;

@Repository
public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    long countByStatus(EventStatus status);
    boolean existsBySchemaId(UUID schemaId);
    Page<LiveEvent> findByStatusNot(EventStatus status, Pageable pageable);
}
