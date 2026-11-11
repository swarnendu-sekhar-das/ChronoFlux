package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    long countByStatus(String status);
    long countByStatusIn(List<String> statuses);
}
