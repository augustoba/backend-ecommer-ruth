package com.estilospequenos.controller;

import com.estilospequenos.dto.SiteSettingsDtos.SettingsRequest;
import com.estilospequenos.dto.SiteSettingsDtos.SettingsResponse;
import com.estilospequenos.service.SiteSettingsService;
import jakarta.validation.Valid;
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
    public SettingsResponse adminSettings() {
        return SettingsResponse.from(service.get());
    }

    @PutMapping("/api/admin/settings")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest req) {
        return SettingsResponse.from(service.update(req));
    }
}
