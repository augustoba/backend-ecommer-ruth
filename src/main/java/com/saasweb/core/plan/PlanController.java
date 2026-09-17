package com.saasweb.core.plan;

import com.saasweb.core.plan.PlanAdminDtos.PlanResponse;
import com.saasweb.core.plan.PlanAdminDtos.PlanUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ABM de planes comerciales — sólo superadmin, es un catálogo de
 * plataforma, no algo que edite el admin de una tienda (mismo criterio que
 * {@code TenantController}). Reemplaza el `UPDATE` a mano en `plan_module`
 * que se usaba hasta ahora para prender/apagar módulos (ver PLAN_SAAS.md).
 * No hay alta de planes nuevos todavía — sólo existe el "default"; cuando
 * haga falta un segundo plan real, se suma acá.
 */
@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanResponse> list() {
        return planService.findAll().stream()
                .map(p -> PlanResponse.from(p, planService.countTenantsUsing(p.getId())))
                .toList();
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable String id, @Valid @RequestBody PlanUpdateRequest req) {
        Plan updated = planService.update(id, req.name(), req.maxProducts(), req.maxAdminUsers(),
                req.enabledModules(), req.showPlatformBranding());
        return PlanResponse.from(updated, planService.countTenantsUsing(updated.getId()));
    }
}
