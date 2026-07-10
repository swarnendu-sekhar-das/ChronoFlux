package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.EventSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSchemaRepository extends JpaRepository<EventSchema, Long> {
}
