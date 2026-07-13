package com.example.scheduler_worker.repository;

import com.example.scheduler_worker.domain.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    
    @Query("SELECT e FROM LiveEvent e WHERE e.status = :status AND e.activeFrom <= :now")
    List<LiveEvent> findEventsToActivate(@Param("status") String status, @Param("now") ZonedDateTime now);

    @Query("SELECT e FROM LiveEvent e WHERE e.status = :status AND e.activeUntil <= :now")
    List<LiveEvent> findEventsToExpire(@Param("status") String status, @Param("now") ZonedDateTime now);
}
