package com.estilospequenos.discount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, String> {
    List<Discount> findByKindAndEnabledTrue(Discount.Kind kind);
}
