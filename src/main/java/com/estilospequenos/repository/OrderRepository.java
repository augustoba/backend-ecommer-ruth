package com.estilospequenos.repository;

import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);

    /** Pedidos de un estado con `processedAt` dentro del rango [from, to). Para métricas. */
    List<Order> findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            OrderStatus status, Instant from, Instant to);

    @Query("select coalesce(max(o.number), 0) from Order o")
    long maxNumber();
}
