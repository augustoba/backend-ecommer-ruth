package com.saasweb.core.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public class TenantAdminDtos {

    /**
     * Alta de tenant (ver PLAN_SAAS.md Fase 10, asistente "Crear tienda").
     * Sólo {@code name}/{@code slug}/{@code rubro} son obligatorios — el
     * resto son los datos que el asistente junta ANTES de crear nada (diseño,
     * color, identidad) y se mandan todos juntos recién en el paso final, así
     * la tienda nace ya configurada en vez de crearse vacía y editarse después.
     */
    public record TenantCreateRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "El identificador sólo puede tener minúsculas, números y guiones.") String slug,
            @NotNull Rubro rubro,
            @Pattern(regexp = "^$|^(classic|minimal|boutique|curva|grid|mercado)$", message = "Diseño desconocido")
            String layout,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String brandColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String headerColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String footerColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String textColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String pageBackgroundColor,
            @Pattern(regexp = "^$|^\\d{8,15}$", message = "WhatsApp: sólo números (8 a 15 dígitos)")
            String whatsappNumber,
            String instagram,
            String facebookUrl,
            @jakarta.validation.constraints.Size(max = 5_000_000) String logoUrl,
            @Pattern(regexp = "^$|^(circle|square|rectangle)$", message = "Forma de logo desconocida")
            String logoShape,
            /**
             * Plan comercial a asignar (Fase 14) — define qué módulos tiene
             * esta tienda (sitio web, punto de venta, o ambos). Vacío/null =
             * el plan por defecto (compat con altas que no pasan por el
             * paso nuevo del asistente).
             */
            String planId) {
    }

    public record TenantResponse(String id, String slug, String name, Rubro rubro, String rubroLabel,
                                 boolean active, Instant createdAt) {
        public static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getSlug(), t.getName(), t.getRubro(),
                    t.getRubro().getLabel(), t.isActive(), t.getCreatedAt());
        }
    }

    /** Pausar/reanudar una tienda (ver TenantResolutionFilter). */
    public record TenantActiveRequest(@NotNull Boolean active) {
    }

    /** Borrado permanente (ver TenantDeletionService) — confirmSlug tiene que ser el slug exacto de la tienda. */
    public record TenantDeleteRequest(@NotBlank String confirmSlug) {
    }
}
