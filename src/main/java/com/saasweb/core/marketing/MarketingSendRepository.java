package com.saasweb.core.marketing;

import com.saasweb.core.marketing.MarketingSend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MarketingSendRepository extends JpaRepository<MarketingSend, String> {

    long countByTenantIdAndSentAtGreaterThanEqualAndSentAtLessThanAndStatus(
            String tenantId, Instant from, Instant to, MarketingSend.Status status);

    /** Ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);

    /** Emails que ya recibieron una campaña (enviada con éxito) desde `cutoff` — para el cooldown. */
    @Query("select s.email from MarketingSend s where s.tenantId = :tenantId "
            + "and s.status = com.saasweb.core.marketing.MarketingSend.Status.SENT and s.sentAt >= :cutoff")
    List<String> emailsSentSince(@Param("tenantId") String tenantId, @Param("cutoff") Instant cutoff);

    @Query("""
            select s from MarketingSend s
            where s.tenantId = :tenantId
              and (:reason is null or s.reason = :reason)
              and (:status is null or s.status = :status)
              and (:from is null or s.sentAt >= :from)
              and (:to is null or s.sentAt < :to)
            order by s.sentAt desc
            """)
    Page<MarketingSend> search(@Param("tenantId") String tenantId,
                                @Param("reason") MarketingSend.Reason reason,
                                @Param("status") MarketingSend.Status status,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);
}
