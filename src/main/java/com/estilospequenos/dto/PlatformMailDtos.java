package com.estilospequenos.dto;

import com.estilospequenos.model.PlatformMailSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class PlatformMailDtos {

    private PlatformMailDtos() {}

    /** `password` en blanco = no cambiarla (se mantiene la que ya estaba guardada). */
    public record PlatformMailSettingsRequest(
            @NotBlank String host,
            @Min(1) @Max(65535) int port,
            @NotBlank String username,
            String password,
            @NotBlank String fromAddress
    ) {}

    /** Nunca devuelve la clave en texto plano — sólo si hay una cargada. */
    public record PlatformMailSettingsResponse(
            String host, int port, String username, String fromAddress, boolean passwordSet
    ) {
        public static PlatformMailSettingsResponse from(PlatformMailSettings s) {
            return new PlatformMailSettingsResponse(s.getHost(), s.getPort(), s.getUsername(),
                    s.getFromAddress(), s.getPassword() != null && !s.getPassword().isBlank());
        }
    }
}
