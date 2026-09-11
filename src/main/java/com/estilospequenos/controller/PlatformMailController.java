package com.estilospequenos.controller;

import com.estilospequenos.dto.PlatformMailDtos.PlatformMailSettingsRequest;
import com.estilospequenos.dto.PlatformMailDtos.PlatformMailSettingsResponse;
import com.estilospequenos.service.PlatformMailSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Credenciales del servicio de mail (SMTP). Sólo superadmin. */
@RestController
@RequestMapping("/api/admin/platform/mail")
public class PlatformMailController {

    private final PlatformMailSettingsService service;

    public PlatformMailController(PlatformMailSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_SETTINGS_MANAGE')")
    public PlatformMailSettingsResponse get() {
        return PlatformMailSettingsResponse.from(service.get());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PLATFORM_SETTINGS_MANAGE')")
    public PlatformMailSettingsResponse update(@Valid @RequestBody PlatformMailSettingsRequest req) {
        return PlatformMailSettingsResponse.from(service.update(req));
    }
}
