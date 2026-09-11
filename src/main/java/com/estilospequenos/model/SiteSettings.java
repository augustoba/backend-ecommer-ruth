package com.estilospequenos.model;

import jakarta.persistence.*;

/**
 * Datos del local editables desde el panel (nombre, WhatsApp, "sobre nosotros",
 * redes). Una sola fila (id fijo). Así el dueño/a puede cambiar el número de
 * WhatsApp o las redes sin redesplegar nada.
 */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

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
