package com.saasweb.core.plan;

import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Mientras haya un único plan (ver PLAN_SAAS.md Fase 5), este service sólo
 * garantiza que exista (sembrado permisivo, sin límites reales) y lo
 * resuelve para el tenant actual. Cargar planes de verdad (Básico,
 * Profesional...) el día que se definan límites/precios es dato, no código.
 */
@Service
@Transactional
public class PlanService {

    public static final String DEFAULT_SLUG = "default";

    private final PlanRepository repo;
    private final TenantRepository tenants;

    public PlanService(PlanRepository repo, TenantRepository tenants) {
        this.repo = repo;
        this.tenants = tenants;
    }

    /** Crea el plan único de este deploy si todavía no existe. Idempotente. */
    public Plan ensureDefault() {
        return repo.findBySlug(DEFAULT_SLUG).orElseGet(() -> {
            Plan p = new Plan();
            p.setId(UUID.randomUUID().toString());
            p.setSlug(DEFAULT_SLUG);
            p.setName("Plan por defecto");
            p.setMaxProducts(null); // sin límite: todavía no hay planes de negocio definidos
            p.setMaxAdminUsers(null);
            p.setEnabledModules(Set.of("ropa"));
            p.setShowPlatformBranding(false);
            return repo.save(p);
        });
    }

    /** Plan del tenant actual (vía TenantContext). null si el tenant no tiene uno asignado. */
    @Transactional(readOnly = true)
    public Plan getCurrent() {
        return getForTenant(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Plan getForTenant(String tenantId) {
        if (tenantId == null) return null;
        String planId = tenants.findById(tenantId).map(Tenant::getPlanId).orElse(null);
        return planId != null ? repo.findById(planId).orElse(null) : null;
    }
}
