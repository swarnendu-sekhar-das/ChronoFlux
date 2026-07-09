package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveEventRepository extends JpaRepository<LiveEvent, Long> {
}
