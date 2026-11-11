package com.example.scheduler_worker.repository;

import com.example.scheduler_worker.domain.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    
    /**
     * Polls events eligible for activation:
     * - Uses Postgres CURRENT_TIMESTAMP to eliminate JVM clock drift across worker pods
     * - Guards against expired windows (active_until > CURRENT_TIMESTAMP)
     * - Enforces deterministic ORDER BY active_from ASC to match index range scan
     * - FOR UPDATE SKIP LOCKED enables lock-free competing workers
     */
    @Query(value = "SELECT * FROM live_events " +
                   "WHERE status = :status " +
                   "  AND active_from <= CURRENT_TIMESTAMP " +
                   "  AND (active_until IS NULL OR active_until > CURRENT_TIMESTAMP) " +
                   "ORDER BY active_from ASC " +
                   "FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<LiveEvent> findEventsToActivate(@Param("status") String status, @Param("limit") int limit);

    /**
     * Polls events eligible for expiration:
     * - Uses Postgres CURRENT_TIMESTAMP
     * - Enforces ORDER BY active_until ASC for deterministic order
     * - FOR UPDATE SKIP LOCKED avoids contention across workers
     */
    @Query(value = "SELECT * FROM live_events " +
                   "WHERE status = :status " +
                   "  AND active_until <= CURRENT_TIMESTAMP " +
                   "ORDER BY active_until ASC " +
                   "FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<LiveEvent> findEventsToExpire(@Param("status") String status, @Param("limit") int limit);
}
