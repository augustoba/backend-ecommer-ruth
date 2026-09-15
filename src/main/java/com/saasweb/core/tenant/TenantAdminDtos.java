package com.saasweb.core.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public class TenantAdminDtos {

    public record TenantCreateRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "El identificador sólo puede tener minúsculas, números y guiones.") String slug,
            @NotNull Rubro rubro) {
    }

    public record TenantResponse(String id, String slug, String name, Rubro rubro, String rubroLabel,
                                 Instant createdAt) {
        public static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getSlug(), t.getName(), t.getRubro(),
                    t.getRubro().getLabel(), t.getCreatedAt());
        }
    }
}
