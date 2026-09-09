package com.estilospequenos.repository;

import com.estilospequenos.model.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ExchangeRepository extends JpaRepository<Exchange, String> {

    List<Exchange> findAllByOrderByCreatedAtDesc();

    List<Exchange> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    @Query("select coalesce(max(e.number), 0) from Exchange e")
    long maxNumber();
}
