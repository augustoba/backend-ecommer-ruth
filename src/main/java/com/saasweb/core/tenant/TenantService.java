package com.saasweb.core.tenant;

import com.saasweb.common.BadRequestException;
import com.saasweb.config.AppProperties;
import com.saasweb.core.plan.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD mínimo de tenants. Hasta la Fase 8 (dominios reales) no hay
 * resolución por `Host` — {@code resolveCurrentTenantId()} sigue
 * resolviendo siempre "el único tenant activo", y
 * {@code TenantResolutionFilter} sólo se aparta de eso con el selector de
 * tienda modo demo (`X-Demo-Tenant`, ver PLAN_SAAS.md) para poder mostrar
 * varios tenants locales sin subdominios reales.
 */
@Service
@Transactional
public class TenantService {

    private final TenantRepository repo;
    private final AppProperties props;
    private final PlanService planService;

    public TenantService(TenantRepository repo, AppProperties props, PlanService planService) {
        this.repo = repo;
        this.props = props;
        this.planService = planService;
    }

    /**
     * Crea el tenant único de este deploy si todavía no existe, con el plan
     * por defecto ya asignado (ver PlanService.ensureDefault) — Tenant.planId
     * es NOT NULL, así que el plan se resuelve/crea en la misma operación,
     * nunca queda una fila de tenant sin plan. Idempotente.
     */
    public Tenant ensureDefault() {
        String slug = props.getTenant().getSlug();
        return repo.findBySlug(slug).orElseGet(() -> create(props.getTenant().getName(), slug, Rubro.ROPA));
    }

    /** Crea un tenant nuevo. No siembra parametrías/productos — eso lo hace TenantProvisioningService. */
    public Tenant create(String name, String slug, Rubro rubro) {
        if (repo.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Ya existe una tienda con ese identificador.");
        }
        Tenant t = new Tenant();
        t.setId(UUID.randomUUID().toString());
        t.setSlug(slug);
        t.setName(name);
        t.setRubro(rubro);
        t.setPlanId(planService.ensureDefault().getId());
        return repo.save(t);
    }

    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    /** Id del tenant que debe usar la request actual. Hoy: siempre el único activo (sin el selector de demo). */
    @Transactional(readOnly = true)
    public String resolveCurrentTenantId() {
        return repo.findFirstByActiveTrueOrderByCreatedAtAsc().map(Tenant::getId).orElse(null);
    }

    /** Para el selector de tienda modo demo (ver TenantResolutionFilter). null si el slug no existe. */
    @Transactional(readOnly = true)
    public String resolveIdBySlug(String slug) {
        return repo.findBySlug(slug).map(Tenant::getId).orElse(null);
    }
}
