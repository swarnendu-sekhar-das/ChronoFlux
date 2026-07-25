package com.example.edge_api.repository;

import com.example.edge_api.domain.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    org.springframework.data.domain.Page<LiveEvent> findByStatusOrderByCreatedAtDesc(String status, org.springframework.data.domain.Pageable pageable);
}
