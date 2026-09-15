package com.saasweb.repository;

import com.saasweb.model.ParamGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParamRepository extends JpaRepository<ParamGroup, String> {
    Optional<ParamGroup> findByIdAndTenantId(String id, String tenantId);
    List<ParamGroup> findByTenantId(String tenantId);
}
