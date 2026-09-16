package com.saasweb.core.plan;

import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
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
        Plan p = repo.findBySlug(DEFAULT_SLUG).orElseGet(() -> {
            Plan created = new Plan();
            created.setId(UUID.randomUUID().toString());
            created.setSlug(DEFAULT_SLUG);
            created.setName("Plan por defecto");
            created.setMaxProducts(null); // sin límite: todavía no hay planes de negocio definidos
            created.setMaxAdminUsers(null);
            created.setEnabledModules(new LinkedHashSet<>(Set.of("ropa", Modules.SOCIAL_SHARE, Modules.MERCADOPAGO)));
            created.setShowPlatformBranding(false);
            return repo.save(created);
        });
        // Backfill de módulos agregados después del primer arranque (mismo
        // criterio que DataSeeder.backfillHeroSlidesAndLogos): un módulo
        // nuevo no debe quedar desactivado en silencio para el plan que ya
        // estaba sembrado. Agregar acá cada módulo nuevo que sume `Modules`.
        Set<String> knownModules = Set.of(Modules.SOCIAL_SHARE, Modules.MERCADOPAGO);
        if (!p.getEnabledModules().containsAll(knownModules)) {
            Set<String> next = new LinkedHashSet<>(p.getEnabledModules());
            next.addAll(knownModules);
            p.setEnabledModules(next);
            p = repo.save(p);
        }
        return p;
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
