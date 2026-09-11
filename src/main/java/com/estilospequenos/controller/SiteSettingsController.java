package com.estilospequenos.controller;

import com.estilospequenos.dto.SiteSettingsDtos.CloudinaryConfigRequest;
import com.estilospequenos.dto.SiteSettingsDtos.CloudinaryConfigResponse;
import com.estilospequenos.dto.SiteSettingsDtos.MailConfigRequest;
import com.estilospequenos.dto.SiteSettingsDtos.MailConfigResponse;
import com.estilospequenos.dto.SiteSettingsDtos.SettingsRequest;
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
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public SettingsResponse adminSettings() {
        return SettingsResponse.from(service.get());
    }

    @PutMapping("/api/admin/settings")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest req) {
        return SettingsResponse.from(service.update(req));
    }

    /**
     * Config de Cloudinary (cuenta usada para subir fotos desde el panel). Separada
     * del resto de `site_settings` porque sólo un superadmin la puede tocar — no es
     * un {@code Permission} normal, así que no se puede otorgar desde `/admin/usuarios`.
     */
    @GetMapping("/api/admin/settings/cloudinary")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public CloudinaryConfigResponse cloudinaryConfig() {
        return CloudinaryConfigResponse.from(service.get());
    }

    @PutMapping("/api/admin/settings/cloudinary")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public CloudinaryConfigResponse updateCloudinaryConfig(@Valid @RequestBody CloudinaryConfigRequest req) {
        return CloudinaryConfigResponse.from(service.updateCloudinary(req.cloudName(), req.uploadPreset()));
    }

    /**
     * Config de SMTP (recuperación de cuenta por mail). Igual que Cloudinary:
     * pensado para que otro sitio armado sobre esta misma base sólo tenga que
     * cargar sus propias credenciales acá, sin tocar código.
     */
    @GetMapping("/api/admin/settings/mail")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public MailConfigResponse mailConfig() {
        return MailConfigResponse.from(service.get());
    }

    @PutMapping("/api/admin/settings/mail")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public MailConfigResponse updateMailConfig(@Valid @RequestBody MailConfigRequest req) {
        return MailConfigResponse.from(
                service.updateMailConfig(req.host(), req.port(), req.username(), req.password(),
                        req.fromEmail(), req.fromName()));
    }
}
