package com.estilospequenos.service;

import com.estilospequenos.dto.SiteSettingsDtos.PaymentsSettingsRequest;
import com.estilospequenos.dto.SiteSettingsDtos.PlatformSettingsRequest;
import com.estilospequenos.model.SiteSettings;
import com.estilospequenos.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteSettingsService {

    private final SiteSettingsRepository repo;

    public SiteSettingsService(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    /** Devuelve la fila de settings; si no existe, la crea con los valores por defecto. */
    public SiteSettings get() {
        return repo.findById(SiteSettings.SINGLETON_ID).orElseGet(() -> repo.save(defaults()));
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
        s.setWhatsappIntro(blankToNull(req.whatsappIntro()));
        s.setWhatsappClosing(blankToNull(req.whatsappClosing()));
        s.setStoreAddress(blankToNull(req.storeAddress()));
        s.setHelpText(blankToNull(req.helpText()));
        s.setFaqText(blankToNull(req.faqText()));
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

    private static SiteSettings defaults() {
        SiteSettings s = new SiteSettings();
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
