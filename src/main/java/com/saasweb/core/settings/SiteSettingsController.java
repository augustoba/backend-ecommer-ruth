package com.saasweb.core.settings;

import com.saasweb.core.settings.SiteSettingsDtos.AppearanceRequest;
import com.saasweb.core.settings.SiteSettingsDtos.CloudinaryConfigRequest;
import com.saasweb.core.settings.SiteSettingsDtos.CloudinaryConfigResponse;
import com.saasweb.core.settings.SiteSettingsDtos.MailConfigRequest;
import com.saasweb.core.settings.SiteSettingsDtos.MailConfigResponse;
import com.saasweb.core.settings.SiteSettingsDtos.MercadoPagoConfigRequest;
import com.saasweb.core.settings.SiteSettingsDtos.MercadoPagoConfigResponse;
import com.saasweb.core.settings.SiteSettingsDtos.PaymentsSettingsRequest;
import com.saasweb.core.settings.SiteSettingsDtos.PlatformSettingsRequest;
import com.saasweb.core.settings.SiteSettingsDtos.SettingsResponse;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.core.plan.Modules;
import com.saasweb.core.plan.PlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class SiteSettingsController {

    private final SiteSettingsService service;
    private final PlanService planService;

    public SiteSettingsController(SiteSettingsService service, PlanService planService) {
        this.service = service;
        this.planService = planService;
    }

    /** true si el plan del tenant actual habilita ese módulo (ver {@link Modules}). */
    private boolean socialShareEnabled() {
        var plan = planService.getCurrent();
        return plan != null && plan.hasModule(Modules.SOCIAL_SHARE);
    }

    /** true sólo si el plan lo habilita Y el tenant activó el checkout Y ya cargó su Access Token. */
    private boolean mercadoPagoAvailable(SiteSettings s) {
        var plan = planService.getCurrent();
        boolean moduleOn = plan != null && plan.hasModule(Modules.MERCADOPAGO);
        return moduleOn && s.isMpEnabled() && s.getMpAccessToken() != null && !s.getMpAccessToken().isBlank();
    }

    private SettingsResponse toResponse(SiteSettings s) {
        return SettingsResponse.from(s, socialShareEnabled(), mercadoPagoAvailable(s));
    }

    /** Público: datos del local para el header, footer y el link de WhatsApp. */
    @GetMapping("/api/settings")
    public SettingsResponse publicSettings() {
        return toResponse(service.get());
    }

    @GetMapping("/api/admin/settings")
    @PreAuthorize("hasAnyAuthority('PLATFORM_SETTINGS_MANAGE', 'PAYMENTS_MANAGE')")
    public SettingsResponse adminSettings() {
        return toResponse(service.get());
    }

    /** Identidad, logo, WhatsApp, redes, textos. Sólo superadmin. */
    @PutMapping("/api/admin/settings/platform")
    @PreAuthorize("hasAuthority('PLATFORM_SETTINGS_MANAGE')")
    public SettingsResponse updatePlatform(@Valid @RequestBody PlatformSettingsRequest req) {
        return toResponse(service.updatePlatform(req));
    }

    /** Diseño de página + color de marca (ver PLAN_SAAS.md Fase 10). Mismo nivel que identidad/logo. */
    @PutMapping("/api/admin/settings/appearance")
    @PreAuthorize("hasAuthority('PLATFORM_SETTINGS_MANAGE')")
    public SettingsResponse updateAppearance(@Valid @RequestBody AppearanceRequest req) {
        return toResponse(service.updateAppearance(req));
    }

    /** Medios de pago. Lo edita el admin normal de la tienda. */
    @PutMapping("/api/admin/settings/payments")
    @PreAuthorize("hasAuthority('PAYMENTS_MANAGE')")
    public SettingsResponse updatePayments(@Valid @RequestBody PaymentsSettingsRequest req) {
        return toResponse(service.updatePayments(req));
    }

    /**
     * Credenciales de Mercado Pago DEL TENANT (Fase 13) — a diferencia de
     * Cloudinary/SMTP, no requiere SUPERADMIN: es la cuenta del propio
     * dueño de la tienda, mismo nivel que el resto de medios de pago.
     */
    @GetMapping("/api/admin/settings/mercadopago")
    @PreAuthorize("hasAuthority('PAYMENTS_MANAGE')")
    public MercadoPagoConfigResponse mercadoPagoConfig() {
        return MercadoPagoConfigResponse.from(service.get());
    }

    @PutMapping("/api/admin/settings/mercadopago")
    @PreAuthorize("hasAuthority('PAYMENTS_MANAGE')")
    public MercadoPagoConfigResponse updateMercadoPagoConfig(@Valid @RequestBody MercadoPagoConfigRequest req) {
        return MercadoPagoConfigResponse.from(service.updateMercadoPago(req));
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
