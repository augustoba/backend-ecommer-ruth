package com.saasweb.repository;

import com.saasweb.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    Optional<Supplier> findByIdAndTenantId(String id, String tenantId);
    List<Supplier> findByTenantId(String tenantId);
}
