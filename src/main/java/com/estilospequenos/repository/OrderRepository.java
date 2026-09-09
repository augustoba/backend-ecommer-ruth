package com.estilospequenos.repository;

import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Listado del panel con filtros opcionales: estado, rango de fechas de
     * creación, y texto (nombre del cliente o número de pedido).
     */
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:from is null or o.createdAt >= :from)
              and (:to is null or o.createdAt < :to)
              and (:search is null
                   or lower(o.customerName) like :searchLike
                   or o.number = :searchNum)
            order by o.createdAt desc
            """)
    Page<Order> search(@Param("status") OrderStatus status,
                       @Param("from") Instant from,
                       @Param("to") Instant to,
                       @Param("search") String search,
                       @Param("searchLike") String searchLike,
                       @Param("searchNum") long searchNum,
                       Pageable pageable);

    java.util.Optional<Order> findByNumber(long number);

    long countByStatus(OrderStatus status);

    /** Pedidos de un estado con `processedAt` dentro del rango [from, to). Para métricas. */
    List<Order> findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            OrderStatus status, Instant from, Instant to);

    @Query("select coalesce(max(o.number), 0) from Order o")
    long maxNumber();
}
