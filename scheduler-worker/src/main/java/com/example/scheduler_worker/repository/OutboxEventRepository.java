package com.example.scheduler_worker.repository;

import com.example.scheduler_worker.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' AND created_at < :cutoff ORDER BY created_at ASC FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<OutboxEvent> findPendingOutboxEvents(@Param("cutoff") ZonedDateTime cutoff, @Param("limit") int limit);
}

