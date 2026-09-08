package com.estilospequenos.repository;

import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    @Query("select coalesce(max(o.number), 0) from Order o")
    long maxNumber();
}
