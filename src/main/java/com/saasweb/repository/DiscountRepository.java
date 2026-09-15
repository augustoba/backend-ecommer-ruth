package com.saasweb.repository;

import com.saasweb.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, String> {
    Optional<Discount> findByIdAndTenantId(String id, String tenantId);
    List<Discount> findByTenantId(String tenantId);
}
