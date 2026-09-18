package com.estilospequenos.repository;

import com.estilospequenos.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId);

    List<StockMovement> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Instant from, Instant to);

    List<StockMovement> findByProductIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String productId, Instant from, Instant to);
}
