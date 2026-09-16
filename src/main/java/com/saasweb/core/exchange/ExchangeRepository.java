package com.saasweb.core.exchange;

import com.saasweb.core.exchange.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<Exchange, String> {

    Optional<Exchange> findByIdAndTenantId(String id, String tenantId);

    /** Borra los cambios del tenant (y sus líneas, cascade ALL/orphanRemoval) — ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);

    List<Exchange> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<Exchange> findByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String tenantId, Instant from, Instant to);

    /** Igual, pero sólo lo que procesó un usuario puntual. Para la caja de un turno. */
    List<Exchange> findByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndProcessedByDni(
            String tenantId, Instant from, Instant to, String processedByDni);

    @Query("select coalesce(max(e.number), 0) from Exchange e where e.tenantId = :tenantId")
    long maxNumber(@Param("tenantId") String tenantId);
}
