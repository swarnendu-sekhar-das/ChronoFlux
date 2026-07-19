package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    long countByStatus(com.chronoflux.adminapi.model.EventStatus status);
}
