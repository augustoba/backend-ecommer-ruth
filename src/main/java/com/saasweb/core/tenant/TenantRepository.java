package com.saasweb.core.tenant;

import com.saasweb.core.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findFirstByActiveTrueOrderByCreatedAtAsc();

    List<Tenant> findAllByOrderByCreatedAtAsc();

    boolean existsByIdAndActiveTrue(String id);

    /** Cuántas tiendas usan este plan — contexto antes de cambiarle límites/módulos (ver PlanController). */
    long countByPlanId(String planId);
}
