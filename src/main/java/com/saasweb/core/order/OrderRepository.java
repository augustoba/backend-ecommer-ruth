package com.saasweb.core.order;

import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    /** Proyección para segmentar clientes por email (marketing): última compra y gasto total. */
    interface CustomerAggregateRow {
        String getEmail();
        Instant getLastOrderAt();
        BigDecimal getLifetimeSpend();
    }

    Optional<Order> findByIdAndTenantId(String id, String tenantId);

    /** Borra los pedidos del tenant (y sus líneas, cascade ALL/orphanRemoval) — ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);

    List<Order> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Page<Order> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * Listado del panel con filtros opcionales: estado, rango de fechas de
     * creación, y texto (nombre del cliente o número de pedido). Siempre
     * acotado al tenant actual.
     */
    @Query("""
            select o from Order o
            where o.tenantId = :tenantId
              and (:status is null or o.status = :status)
              and (:from is null or o.createdAt >= :from)
              and (:to is null or o.createdAt < :to)
              and (:search is null
                   or lower(o.customerName) like :searchLike
                   or o.number = :searchNum)
            order by o.createdAt desc
            """)
    Page<Order> search(@Param("tenantId") String tenantId,
                       @Param("status") OrderStatus status,
                       @Param("from") Instant from,
                       @Param("to") Instant to,
                       @Param("search") String search,
                       @Param("searchLike") String searchLike,
                       @Param("searchNum") long searchNum,
                       Pageable pageable);

    Optional<Order> findByTenantIdAndNumber(String tenantId, long number);

    /** Pedidos procesados de un tenant, sin filtro de fecha — usado por el backfill de costo histórico. */
    List<Order> findByTenantIdAndStatus(String tenantId, OrderStatus status);

    long countByTenantIdAndStatus(String tenantId, OrderStatus status);

    /** Pedidos de un estado con `processedAt` dentro del rango [from, to). Para métricas. */
    List<Order> findByTenantIdAndStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            String tenantId, OrderStatus status, Instant from, Instant to);

    /** Igual, pero sólo lo que confirmó/cobró un usuario puntual. Para la caja de un turno. */
    List<Order> findByTenantIdAndStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThanAndConfirmedByDni(
            String tenantId, OrderStatus status, Instant from, Instant to, String confirmedByDni);

    @Query("select coalesce(max(o.number), 0) from Order o where o.tenantId = :tenantId")
    long maxNumber(@Param("tenantId") String tenantId);

    /** Segmento "inactivos": clientes cuyo último pedido procesado es anterior a `cutoff`. */
    @Query("""
            select o.customerEmail as email, max(o.processedAt) as lastOrderAt, sum(o.total) as lifetimeSpend
            from Order o
            where o.tenantId = :tenantId
              and o.status = com.saasweb.core.order.OrderStatus.PROCESADO
              and o.customerEmail is not null
            group by o.customerEmail
            having max(o.processedAt) < :cutoff
            """)
    List<CustomerAggregateRow> findInactiveCustomers(@Param("tenantId") String tenantId, @Param("cutoff") Instant cutoff);

    /** Segmento "VIP": clientes cuyo gasto acumulado (pedidos procesados) supera `threshold`. */
    @Query("""
            select o.customerEmail as email, max(o.processedAt) as lastOrderAt, sum(o.total) as lifetimeSpend
            from Order o
            where o.tenantId = :tenantId
              and o.status = com.saasweb.core.order.OrderStatus.PROCESADO
              and o.customerEmail is not null
            group by o.customerEmail
            having sum(o.total) > :threshold
            """)
    List<CustomerAggregateRow> findHighSpendCustomers(@Param("tenantId") String tenantId, @Param("threshold") BigDecimal threshold);
}
