package com.saasweb.core.tenant;

import com.saasweb.core.tenant.TenantAdminDtos.TenantCreateRequest;
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

    public TenantController(TenantProvisioningService provisioningService, TenantService tenantService) {
        this.provisioningService = provisioningService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.findAll().stream().map(TenantResponse::from).toList();
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
        Tenant t = provisioningService.provision(req.name().trim(), req.slug().trim(), req.rubro());
        return TenantResponse.from(t);
    }
}
