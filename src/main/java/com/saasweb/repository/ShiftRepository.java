package com.saasweb.repository;

import com.saasweb.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, String> {

    Optional<Shift> findByIdAndTenantId(String id, String tenantId);

    Optional<Shift> findByTenantIdAndUserDniAndClosedAtIsNull(String tenantId, String userDni);

    Page<Shift> findByTenantIdOrderByOpenedAtDesc(String tenantId, Pageable pageable);

    Page<Shift> findByTenantIdAndUserDniOrderByOpenedAtDesc(String tenantId, String userDni, Pageable pageable);
}
