package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
}
