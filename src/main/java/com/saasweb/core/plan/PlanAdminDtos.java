package com.saasweb.core.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** DTOs de administración de planes (ver PlanController) — sólo superadmin. */
public final class PlanAdminDtos {

    private PlanAdminDtos() {
    }

    public record PlanResponse(
            String id, String slug, String name,
            Integer maxProducts, Integer maxAdminUsers,
            Set<String> enabledModules, boolean showPlatformBranding,
            /** Cuántas tiendas usan este plan hoy — contexto antes de cambiarle límites/módulos. */
            long tenantCount
    ) {
        public static PlanResponse from(Plan p, long tenantCount) {
            return new PlanResponse(p.getId(), p.getSlug(), p.getName(),
                    p.getMaxProducts(), p.getMaxAdminUsers(),
                    p.getEnabledModules(), p.isShowPlatformBranding(), tenantCount);
        }
    }

    /**
     * {@code maxProducts}/{@code maxAdminUsers} en {@code null} = sin límite.
     * El slug no se edita (es el identificador estable del plan).
     */
    public record PlanUpdateRequest(
            @NotBlank String name,
            Integer maxProducts,
            Integer maxAdminUsers,
            @NotNull Set<String> enabledModules,
            boolean showPlatformBranding
    ) {
    }
}
