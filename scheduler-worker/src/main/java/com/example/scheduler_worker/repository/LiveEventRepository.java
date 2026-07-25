package com.example.scheduler_worker.repository;

import com.example.scheduler_worker.domain.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    
    @Query(value = "SELECT * FROM live_events WHERE status = :status AND active_from <= :now FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<LiveEvent> findEventsToActivate(@Param("status") String status, @Param("now") ZonedDateTime now, @Param("limit") int limit);

    @Query(value = "SELECT * FROM live_events WHERE status = :status AND active_until <= :now FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<LiveEvent> findEventsToExpire(@Param("status") String status, @Param("now") ZonedDateTime now, @Param("limit") int limit);
}
