package com.saasweb.core.settings;

import jakarta.persistence.*;

/**
 * Datos del local editables desde el panel (nombre, WhatsApp, "sobre nosotros",
 * redes). Una fila por tenant — el id de la fila ES el id del tenant (relación
 * 1:1), así el dueño/a puede cambiar el número de WhatsApp o las redes sin
 * redesplegar nada.
 */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    /** Id del tenant dueño de esta config (ver TenantService/SiteSettingsService.get()). */
    @Id
    private String id;

    @Column(nullable = false)
    private String storeName;

    /** Número de WhatsApp en formato internacional sin +, espacios ni 15. */
    @Column(nullable = false)
    private String whatsappNumber;

    @Column(length = 2000)
    private String aboutText;

    /** Usuario de Instagram, sin @. */
    private String instagram;

    private String facebookUrl;

    /** Logo del negocio: URL o data URI. null = usar el logo por defecto (`logo.jpeg`). */
    @Column(length = 5_000_000)
    private String logoUrl;

    /**
     * Forma en la que se recorta el logo dondequiera que se muestre (header,
     * pie de página, portada de Clásico): `"circle"` (por defecto),
     * `"square"` o `"rectangle"`. Nullable a propósito — null se trata como
     * `"circle"` (el único recorte que existía antes de este campo) en
     * {@code SettingsResponse}, así ningún logo ya cargado cambia de golpe.
     */
    private String logoShape;

    /**
     * Theme visual del sitio (ver PLAN_SAAS.md Fase 6). Hoy sólo existe
     * `"default"` — el campo es la base para poder ofrecer más de una
     * apariencia sin redeploy el día que haya una segunda. Nullable a
     * propósito (no todo tenant existente tiene por qué tener el valor
     * seteado): null se trata como `"default"` en {@code SettingsResponse}.
     */
    private String theme;

    /**
     * Diseño de página elegido (ver PLAN_SAAS.md Fase 10) — eje independiente
     * de {@code theme}: mientras {@code theme} sólo cambia colores/tipografía,
     * este campo elige entre layouts realmente distintos (estructura de
     * home/catálogo). Nullable a propósito: null se trata como `"classic"`
     * (el único layout que existe hoy) en {@code SettingsResponse}.
     */
    private String layout;

    /**
     * Color de marca elegido libremente por el tenant (hex, ej. `"#e8432a"`),
     * ver PLAN_SAAS.md Fase 10. Cuando no es null, el frontend deriva la
     * rampa `--color-brand-50..700` a partir de este valor y la aplica en
     * runtime, por encima del `[data-theme]` con nombre de `theme`. Null =
     * seguir usando la paleta con nombre de siempre (cero cambio para los
     * tenants existentes).
     */
    private String brandColor;

    /**
     * Colores independientes de {@code brandColor} (ver PLAN_SAAS.md Fase 10
     * ampliada): cada uno pisa sólo su propia zona (encabezado, pie de
     * página, texto de títulos, fondo de página) — null = seguir derivando
     * ese color de la rampa de {@code brandColor}/del layout, como siempre.
     */
    private String headerColor;
    private String footerColor;
    private String textColor;
    private String pageBackgroundColor;

    /**
     * Texto de saludo del mensaje de pedido de WhatsApp (antes del detalle).
     * Admite los tokens {tienda} y {codigo}. null = usar el texto por defecto.
     */
    @Column(length = 2000)
    private String whatsappIntro;

    /**
     * Texto de cierre del mensaje de pedido de WhatsApp (después de los totales),
     * ej: cómo coordinar el pago. Admite {tienda} y {codigo}. null = default.
     */
    @Column(length = 2000)
    private String whatsappClosing;

    /** Dirección del local (para la opción "Retiro en el local" del checkout). */
    @Column(length = 500)
    private String storeAddress;

    /**
     * Texto de la página "Cómo comprar" (texto libre, se respeta el salto de
     * línea). `length` grande → Hibernate lo mapea a MEDIUMTEXT (no entra en el
     * límite de tamaño de fila de MySQL como haría un VARCHAR grande).
     */
    @Column(length = 100_000)
    private String helpText;

    /**
     * Preguntas frecuentes, texto libre. Cada bloque separado por una línea en
     * blanco: la primera línea es la pregunta, el resto la respuesta. MEDIUMTEXT.
     */
    @Column(length = 500_000)
    private String faqText;

    // --- Medios de pago (aparece en el checkout si está habilitado Y tiene su dato) ---

    @Column(nullable = false)
    private boolean paymentTransferEnabled = false;
    /** Alias/CBU para transferencia. */
    @Column(length = 200)
    private String paymentTransferAlias;

    @Column(nullable = false)
    private boolean paymentQrTransferEnabled = false;
    /** Imagen (data URI) del QR de transferencia. */
    @Column(length = 5_000_000)
    private String paymentQrTransferImage;

    @Column(nullable = false)
    private boolean paymentQrCardEnabled = false;
    /** Imagen (data URI) del QR de pago con tarjeta. */
    @Column(length = 5_000_000)
    private String paymentQrCardImage;
    /** Link de cobro con tarjeta (ej. Mercado Pago). Alternativa/complemento al QR. */
    @Column(length = 1000)
    private String paymentCardLink;

    /** Habilita "efectivo al recibir/retirar". */
    @Column(nullable = false)
    private boolean paymentCashEnabled = false;

    // --- Cloudinary (subida de imágenes desde el panel) ---
    // Editable solo por superadmin (ver AdminUser.superAdmin); se leen desde el
    // endpoint público de settings porque cualquier sesión de admin las necesita
    // para poder subir fotos. No son secretas (unsigned upload preset).

    @Column(length = 200)
    private String cloudinaryCloudName;

    @Column(length = 200)
    private String cloudinaryUploadPreset;

    public String getCloudinaryCloudName() {
        return cloudinaryCloudName;
    }

    public void setCloudinaryCloudName(String cloudinaryCloudName) {
        this.cloudinaryCloudName = cloudinaryCloudName;
    }

    public String getCloudinaryUploadPreset() {
        return cloudinaryUploadPreset;
    }

    public void setCloudinaryUploadPreset(String cloudinaryUploadPreset) {
        this.cloudinaryUploadPreset = cloudinaryUploadPreset;
    }

    // --- SMTP (recuperación de cuenta por mail) ---
    // Editable solo por superadmin. A diferencia de Cloudinary, `smtpPassword` es
    // secreto de verdad (una API key de Brevo) — nunca se devuelve en ninguna
    // respuesta, sólo se puede pisar (ver CloudinaryConfigResponse vs. MailConfigResponse).

    @Column(length = 300)
    private String smtpHost;

    private Integer smtpPort;

    @Column(length = 300)
    private String smtpUsername;

    @Column(length = 500)
    private String smtpPassword;

    @Column(length = 300)
    private String smtpFromEmail;

    @Column(length = 200)
    private String smtpFromName;

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpFromEmail() {
        return smtpFromEmail;
    }

    public void setSmtpFromEmail(String smtpFromEmail) {
        this.smtpFromEmail = smtpFromEmail;
    }

    public String getSmtpFromName() {
        return smtpFromName;
    }

    public void setSmtpFromName(String smtpFromName) {
        this.smtpFromName = smtpFromName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getAboutText() {
        return aboutText;
    }

    public void setAboutText(String aboutText) {
        this.aboutText = aboutText;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoShape() {
        return logoShape;
    }

    public void setLogoShape(String logoShape) {
        this.logoShape = logoShape;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public void setBrandColor(String brandColor) {
        this.brandColor = brandColor;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public String getFooterColor() {
        return footerColor;
    }

    public void setFooterColor(String footerColor) {
        this.footerColor = footerColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getPageBackgroundColor() {
        return pageBackgroundColor;
    }

    public void setPageBackgroundColor(String pageBackgroundColor) {
        this.pageBackgroundColor = pageBackgroundColor;
    }

    public String getWhatsappIntro() {
        return whatsappIntro;
    }

    public void setWhatsappIntro(String whatsappIntro) {
        this.whatsappIntro = whatsappIntro;
    }

    public String getWhatsappClosing() {
        return whatsappClosing;
    }

    public void setWhatsappClosing(String whatsappClosing) {
        this.whatsappClosing = whatsappClosing;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getFaqText() {
        return faqText;
    }

    public void setFaqText(String faqText) {
        this.faqText = faqText;
    }

    public boolean isPaymentTransferEnabled() {
        return paymentTransferEnabled;
    }

    public void setPaymentTransferEnabled(boolean paymentTransferEnabled) {
        this.paymentTransferEnabled = paymentTransferEnabled;
    }

    public String getPaymentTransferAlias() {
        return paymentTransferAlias;
    }

    public void setPaymentTransferAlias(String paymentTransferAlias) {
        this.paymentTransferAlias = paymentTransferAlias;
    }

    public boolean isPaymentQrTransferEnabled() {
        return paymentQrTransferEnabled;
    }

    public void setPaymentQrTransferEnabled(boolean paymentQrTransferEnabled) {
        this.paymentQrTransferEnabled = paymentQrTransferEnabled;
    }

    public String getPaymentQrTransferImage() {
        return paymentQrTransferImage;
    }

    public void setPaymentQrTransferImage(String paymentQrTransferImage) {
        this.paymentQrTransferImage = paymentQrTransferImage;
    }

    public boolean isPaymentQrCardEnabled() {
        return paymentQrCardEnabled;
    }

    public void setPaymentQrCardEnabled(boolean paymentQrCardEnabled) {
        this.paymentQrCardEnabled = paymentQrCardEnabled;
    }

    public String getPaymentQrCardImage() {
        return paymentQrCardImage;
    }

    public void setPaymentQrCardImage(String paymentQrCardImage) {
        this.paymentQrCardImage = paymentQrCardImage;
    }

    public String getPaymentCardLink() {
        return paymentCardLink;
    }

    public void setPaymentCardLink(String paymentCardLink) {
        this.paymentCardLink = paymentCardLink;
    }

    public boolean isPaymentCashEnabled() {
        return paymentCashEnabled;
    }

    public void setPaymentCashEnabled(boolean paymentCashEnabled) {
        this.paymentCashEnabled = paymentCashEnabled;
    }
}
