package com.saasweb.repository;

import com.saasweb.model.SizeScale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SizeScaleRepository extends JpaRepository<SizeScale, String> {
    Optional<SizeScale> findByIdAndTenantId(String id, String tenantId);
    List<SizeScale> findByTenantId(String tenantId);
}
