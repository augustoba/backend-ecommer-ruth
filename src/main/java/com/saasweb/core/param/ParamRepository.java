package com.saasweb.core.param;

import com.saasweb.core.param.ParamGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParamRepository extends JpaRepository<ParamGroup, String> {
    Optional<ParamGroup> findByIdAndTenantId(String id, String tenantId);
    List<ParamGroup> findByTenantId(String tenantId);
}
