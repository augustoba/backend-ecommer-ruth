package com.estilospequenos.repository;

import com.estilospequenos.model.MarketingSend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MarketingSendRepository extends JpaRepository<MarketingSend, String> {

    long countBySentAtGreaterThanEqualAndSentAtLessThanAndStatus(
            Instant from, Instant to, MarketingSend.Status status);

    /** Emails que ya recibieron una campaña (enviada con éxito) desde `cutoff` — para el cooldown. */
    @Query("select s.email from MarketingSend s where s.status = com.estilospequenos.model.MarketingSend.Status.SENT and s.sentAt >= :cutoff")
    List<String> emailsSentSince(@Param("cutoff") Instant cutoff);

    @Query("""
            select s from MarketingSend s
            where (:reason is null or s.reason = :reason)
              and (:status is null or s.status = :status)
              and (:from is null or s.sentAt >= :from)
              and (:to is null or s.sentAt < :to)
            order by s.sentAt desc
            """)
    Page<MarketingSend> search(@Param("reason") MarketingSend.Reason reason,
                                @Param("status") MarketingSend.Status status,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);
}
