package com.estilospequenos.controller;

import com.estilospequenos.dto.SiteSettingsDtos.PaymentsSettingsRequest;
import com.estilospequenos.dto.SiteSettingsDtos.PlatformSettingsRequest;
import com.estilospequenos.dto.SiteSettingsDtos.SettingsResponse;
import com.estilospequenos.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class SiteSettingsController {

    private final SiteSettingsService service;

    public SiteSettingsController(SiteSettingsService service) {
        this.service = service;
    }

    /** Público: datos del local para el header, footer y el link de WhatsApp. */
    @GetMapping("/api/settings")
    public SettingsResponse publicSettings() {
        return SettingsResponse.from(service.get());
    }

    @GetMapping("/api/admin/settings")
    @PreAuthorize("hasAnyAuthority('PLATFORM_SETTINGS_MANAGE', 'PAYMENTS_MANAGE')")
    public SettingsResponse adminSettings() {
        return SettingsResponse.from(service.get());
    }

    /** Identidad, logo, WhatsApp, redes, textos. Sólo superadmin. */
    @PutMapping("/api/admin/settings/platform")
    @PreAuthorize("hasAuthority('PLATFORM_SETTINGS_MANAGE')")
    public SettingsResponse updatePlatform(@Valid @RequestBody PlatformSettingsRequest req) {
        return SettingsResponse.from(service.updatePlatform(req));
    }

    /** Medios de pago. Lo edita el admin normal de la tienda. */
    @PutMapping("/api/admin/settings/payments")
    @PreAuthorize("hasAuthority('PAYMENTS_MANAGE')")
    public SettingsResponse updatePayments(@Valid @RequestBody PaymentsSettingsRequest req) {
        return SettingsResponse.from(service.updatePayments(req));
    }
}
