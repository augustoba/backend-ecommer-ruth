package com.saasweb.core.tenant;

import com.saasweb.core.plan.Modules;
import com.saasweb.core.plan.Plan;
import com.saasweb.core.plan.PlanService;
import com.saasweb.core.tenant.TenantAdminDtos.TenantActiveRequest;
import com.saasweb.core.tenant.TenantAdminDtos.TenantCreateRequest;
import com.saasweb.core.tenant.TenantAdminDtos.TenantDeleteRequest;
import com.saasweb.core.tenant.TenantAdminDtos.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alta y listado de tiendas (tenants). Sólo superadmin — crear una tienda
 * nueva es un acto de plataforma, no algo que haga el admin de una tienda
 * existente. Pensado para el asistente "Crear tienda" del panel y para el
 * selector de tienda modo demo (ver PLAN_SAAS.md y TenantResolutionFilter).
 */
@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class TenantController {

    private final TenantProvisioningService provisioningService;
    private final TenantService tenantService;
    private final TenantDeletionService deletionService;
    private final PlanService planService;

    public TenantController(TenantProvisioningService provisioningService, TenantService tenantService,
            TenantDeletionService deletionService, PlanService planService) {
        this.provisioningService = provisioningService;
        this.tenantService = tenantService;
        this.deletionService = deletionService;
        this.planService = planService;
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.findAll().stream().map(t -> TenantResponse.from(t, ecommerceEnabled(t))).toList();
    }

    /** Módulo `ECOMMERCE_SITE` del plan de este tenant (ver Fase 17) — decide si tiene sitio público. */
    private boolean ecommerceEnabled(Tenant t) {
        Plan plan = planService.getForTenant(t.getId());
        return plan != null && plan.hasModule(Modules.ECOMMERCE_SITE);
    }

    /** Rubros disponibles para el selector del asistente "Crear tienda" — evita hardcodear el enum en el front. */
    @GetMapping("/rubros")
    public List<RubroOption> rubros() {
        return java.util.Arrays.stream(Rubro.values())
                .map(r -> new RubroOption(r.name(), r.getLabel()))
                .toList();
    }

    public record RubroOption(String value, String label) {
    }

    @PostMapping
    public TenantResponse create(@Valid @RequestBody TenantCreateRequest req) {
        Tenant t = provisioningService.provision(req);
        return TenantResponse.from(t, ecommerceEnabled(t));
    }

    /** Pausar (deja de poder verse el storefront) o reanudar una tienda. */
    @PatchMapping("/{id}/active")
    public TenantResponse setActive(@PathVariable String id, @Valid @RequestBody TenantActiveRequest req) {
        Tenant t = tenantService.setActive(id, req.active());
        return TenantResponse.from(t, ecommerceEnabled(t));
    }

    /**
     * Borrado permanente e irreversible de la tienda y TODOS sus datos (ver
     * TenantDeletionService) — {@code confirmSlug} tiene que ser exactamente
     * el slug de la tienda, tal como lo tipeó el superadmin en el frontend.
     */
    @PostMapping("/{id}/delete")
    public void delete(@PathVariable String id, @Valid @RequestBody TenantDeleteRequest req) {
        deletionService.deleteTenant(id, req.confirmSlug());
    }
}
