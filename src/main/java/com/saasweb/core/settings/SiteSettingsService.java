package com.saasweb.core.settings;

import com.saasweb.common.TenantContext;
import com.saasweb.core.settings.SiteSettingsDtos.PaymentsSettingsRequest;
import com.saasweb.core.settings.SiteSettingsDtos.PlatformSettingsRequest;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteSettingsService {

    private final SiteSettingsRepository repo;

    public SiteSettingsService(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    /**
     * Crea la fila de settings de un tenant nuevo con el nombre elegido —
     * NO hardcodea la marca de Estilos Pequeños (a diferencia de
     * {@link #defaults()}, que es específica de esa tienda piloto). La
     * llama {@code TenantProvisioningService} antes de que nadie pida
     * {@link #get()} para ese tenant.
     */
    public SiteSettings createFor(String tenantId, String storeName, String theme, String layout, String logoUrl) {
        SiteSettings s = new SiteSettings();
        s.setId(tenantId);
        s.setTheme(theme);
        s.setLayout(layout);
        s.setStoreName(storeName);
        s.setWhatsappNumber("5491100000000");
        s.setLogoUrl(logoUrl);
        // Cloudinary es una cuenta de plataforma, no por tenant (ver javadoc del
        // campo) — sin esto, una tienda nueva no puede subir ninguna imagen
        // desde el panel hasta que un superadmin la configure a mano.
        repo.findFirstByCloudinaryCloudNameIsNotNull().ifPresent(existing -> {
            s.setCloudinaryCloudName(existing.getCloudinaryCloudName());
            s.setCloudinaryUploadPreset(existing.getCloudinaryUploadPreset());
        });
        return repo.save(s);
    }

    /**
     * Aplica identidad + color de marca directo por id de tenant — a
     * diferencia de {@link #updatePlatform}/{@link #updateAppearance}, NO
     * pasa por {@link TenantContext} (durante el alta de un tenant nuevo la
     * request todavía está en el contexto del superadmin, no en el del
     * tenant recién creado). La usa {@code TenantProvisioningService} para
     * que el asistente "Crear tienda" pueda mandar todo junto en el alta
     * (ver PLAN_SAAS.md Fase 10). Parámetros null se ignoran.
     */
    /**
     * Todos los campos opcionales que junta el asistente "Crear tienda"
     * ANTES de crear nada (ver PLAN_SAAS.md Fase 10) — un objeto en vez de
     * seguir sumando parámetros String sueltos a `applyOnboardingExtras`.
     * Cada uno, si no es null, pisa el valor por defecto de esa fila recién
     * creada; si es null, queda el default de siempre.
     */
    public record OnboardingExtras(String brandColor, String headerColor, String footerColor, String textColor,
                                    String pageBackgroundColor, String whatsappNumber, String instagram,
                                    String facebookUrl, String logoUrl, String logoShape) {
        public boolean isEmpty() {
            return brandColor == null && headerColor == null && footerColor == null && textColor == null
                    && pageBackgroundColor == null && whatsappNumber == null && instagram == null
                    && facebookUrl == null && logoUrl == null && logoShape == null;
        }
    }

    public void applyOnboardingExtras(String tenantId, OnboardingExtras extras) {
        if (extras.isEmpty()) return;
        SiteSettings s = repo.findById(tenantId).orElseThrow();
        if (extras.brandColor() != null) s.setBrandColor(extras.brandColor());
        if (extras.headerColor() != null) s.setHeaderColor(extras.headerColor());
        if (extras.footerColor() != null) s.setFooterColor(extras.footerColor());
        if (extras.textColor() != null) s.setTextColor(extras.textColor());
        if (extras.pageBackgroundColor() != null) s.setPageBackgroundColor(extras.pageBackgroundColor());
        if (extras.whatsappNumber() != null) s.setWhatsappNumber(extras.whatsappNumber());
        if (extras.instagram() != null) s.setInstagram(cleanHandle(extras.instagram()));
        if (extras.facebookUrl() != null) s.setFacebookUrl(extras.facebookUrl());
        if (extras.logoUrl() != null) s.setLogoUrl(extras.logoUrl());
        if (extras.logoShape() != null) s.setLogoShape(extras.logoShape());
        repo.save(s);
    }

    /** Devuelve la fila de settings del tenant actual; si no existe, la crea con los valores por defecto. */
    public SiteSettings get() {
        String tenantId = TenantContext.getTenantId();
        return repo.findById(tenantId).orElseGet(() -> {
            SiteSettings s = defaults();
            s.setId(tenantId);
            return repo.save(s);
        });
    }

    /** Textos por defecto del mensaje de pedido de WhatsApp (si no se personalizan). */
    public static final String DEFAULT_WHATSAPP_INTRO = "¡Hola! Quiero hacer un pedido en *{tienda}* 🧸";
    public static final String DEFAULT_WHATSAPP_CLOSING =
            "Quedo atento/a a que me pases el alias o el link de Mercado Pago para coordinar el pago. ¡Gracias!";

    public SiteSettings updatePlatform(PlatformSettingsRequest req) {
        SiteSettings s = get();
        s.setStoreName(req.storeName().trim());
        s.setWhatsappNumber(req.whatsappNumber().trim());
        s.setAboutText(blankToNull(req.aboutText()));
        s.setInstagram(cleanHandle(req.instagram()));
        s.setFacebookUrl(blankToNull(req.facebookUrl()));
        s.setLogoUrl(blankToNull(req.logoUrl()));
        s.setLogoShape(blankToNull(req.logoShape()));
        s.setWhatsappIntro(blankToNull(req.whatsappIntro()));
        s.setWhatsappClosing(blankToNull(req.whatsappClosing()));
        s.setStoreAddress(blankToNull(req.storeAddress()));
        s.setHelpText(blankToNull(req.helpText()));
        s.setFaqText(blankToNull(req.faqText()));
        return repo.save(s);
    }

    /** Diseño de página + color de marca (ver PLAN_SAAS.md Fase 10). */
    public SiteSettings updateAppearance(SiteSettingsDtos.AppearanceRequest req) {
        SiteSettings s = get();
        s.setLayout(blankToNull(req.layout()));
        s.setBrandColor(blankToNull(req.brandColor()));
        s.setHeaderColor(blankToNull(req.headerColor()));
        s.setFooterColor(blankToNull(req.footerColor()));
        s.setTextColor(blankToNull(req.textColor()));
        s.setPageBackgroundColor(blankToNull(req.pageBackgroundColor()));
        return repo.save(s);
    }

    public SiteSettings updatePayments(PaymentsSettingsRequest req) {
        SiteSettings s = get();
        s.setPaymentTransferEnabled(Boolean.TRUE.equals(req.paymentTransferEnabled()));
        s.setPaymentTransferAlias(blankToNull(req.paymentTransferAlias()));
        s.setPaymentQrTransferEnabled(Boolean.TRUE.equals(req.paymentQrTransferEnabled()));
        s.setPaymentQrTransferImage(blankToNull(req.paymentQrTransferImage()));
        s.setPaymentQrCardEnabled(Boolean.TRUE.equals(req.paymentQrCardEnabled()));
        s.setPaymentQrCardImage(blankToNull(req.paymentQrCardImage()));
        s.setPaymentCardLink(blankToNull(req.paymentCardLink()));
        s.setPaymentCashEnabled(Boolean.TRUE.equals(req.paymentCashEnabled()));
        return repo.save(s);
    }

    /**
     * Sólo lo puede llamar el controller gateado por la authority
     * PAYMENTS_MANAGE (es la cuenta de Mercado Pago DEL TENANT, no una
     * compartida — a diferencia de Cloudinary/SMTP no hace falta ser
     * superadmin). {@code accessToken} en blanco = no tocar el que ya
     * está guardado.
     */
    public SiteSettings updateMercadoPago(SiteSettingsDtos.MercadoPagoConfigRequest req) {
        SiteSettings s = get();
        s.setMpEnabled(Boolean.TRUE.equals(req.mpEnabled()));
        if (req.accessToken() != null && !req.accessToken().isBlank()) {
            s.setMpAccessToken(req.accessToken().trim());
        }
        s.setMpPublicKey(blankToNull(req.publicKey()));
        return repo.save(s);
    }

    public SiteSettings updateArca(SiteSettingsDtos.ArcaConfigRequest req) {
        SiteSettings s = get();
        s.setArcaEnabled(Boolean.TRUE.equals(req.arcaEnabled()));
        if (req.arcaModoPrueba() != null) s.setArcaModoPrueba(req.arcaModoPrueba());
        s.setArcaCuit(blankToNull(req.cuit()));
        s.setArcaPuntoVenta(req.puntoVenta());
        s.setArcaCondicionIva(blankToNull(req.condicionIva()));
        if (req.certificadoPem() != null && !req.certificadoPem().isBlank()) {
            s.setArcaCertificadoPem(req.certificadoPem().trim());
        }
        if (req.clavePrivadaPem() != null && !req.clavePrivadaPem().isBlank()) {
            s.setArcaClavePrivadaPem(req.clavePrivadaPem().trim());
        }
        if (req.invoiceMode() != null && !req.invoiceMode().isBlank()) {
            s.setInvoiceMode(req.invoiceMode());
        }
        return repo.save(s);
    }

    private static SiteSettings defaults() {
        SiteSettings s = new SiteSettings();
        s.setTheme("default");
        s.setStoreName("Estilos Pequeños");
        s.setWhatsappNumber("5491122334455");
        s.setAboutText(
                "Somos Estilos Pequeños 🧸 Hace 5 años vestimos a los más chicos con ropa cómoda, "
                        + "de calidad y con onda. Elegimos cada prenda pensando en la comodidad de los peques "
                        + "y la tranquilidad de las familias. ¡Gracias por elegirnos!");
        s.setInstagram("estilospequenos_");
        s.setFacebookUrl("https://www.facebook.com/share/1NZXdYgick/");
        s.setWhatsappIntro(DEFAULT_WHATSAPP_INTRO);
        s.setWhatsappClosing(DEFAULT_WHATSAPP_CLOSING);
        // Cuenta de Cloudinary actual (antes hardcodeada en site-config.ts del
        // frontend); el superadmin la puede cambiar desde /admin/superadmin/cloudinary.
        s.setCloudinaryCloudName("jitutkbc");
        s.setCloudinaryUploadPreset("estilospequenos");
        return s;
    }

    /** Sólo lo puede llamar el controller gateado por la authority SUPERADMIN. */
    public SiteSettings updateCloudinary(String cloudName, String uploadPreset) {
        SiteSettings s = get();
        s.setCloudinaryCloudName(blankToNull(cloudName));
        s.setCloudinaryUploadPreset(blankToNull(uploadPreset));
        return repo.save(s);
    }

    /**
     * Sólo lo puede llamar el controller gateado por la authority SUPERADMIN.
     * {@code password} en blanco = no tocar la que ya está guardada.
     */
    public SiteSettings updateMailConfig(String host, Integer port, String username, String password,
                                         String fromEmail, String fromName) {
        SiteSettings s = get();
        s.setSmtpHost(blankToNull(host));
        s.setSmtpPort(port);
        s.setSmtpUsername(blankToNull(username));
        if (password != null && !password.isBlank()) {
            s.setSmtpPassword(password);
        }
        s.setSmtpFromEmail(blankToNull(fromEmail));
        s.setSmtpFromName(blankToNull(fromName));
        return repo.save(s);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String cleanHandle(String v) {
        String h = blankToNull(v);
        return h == null ? null : h.replaceFirst("^@", "");
    }
}
