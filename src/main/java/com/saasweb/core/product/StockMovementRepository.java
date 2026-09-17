package com.saasweb.core.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findByTenantIdAndProductIdOrderByCreatedAtDesc(String tenantId, String productId);

    List<StockMovement> findByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String tenantId, Instant from, Instant to);

    List<StockMovement> findByTenantIdAndProductIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String tenantId, String productId, Instant from, Instant to);

    /** Ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);
}
