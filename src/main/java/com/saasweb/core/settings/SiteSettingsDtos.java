package com.saasweb.core.settings;

import com.saasweb.core.settings.SiteSettings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SiteSettingsDtos {

    private SiteSettingsDtos() {}

    /** Configuración de plataforma (identidad, logo, WhatsApp, redes, textos). Sólo superadmin. */
    public record PlatformSettingsRequest(
            @NotBlank String storeName,
            @NotBlank
            @Pattern(regexp = "\\d{8,15}", message = "Solo números, sin +, espacios ni 15 (8 a 15 dígitos)")
            String whatsappNumber,
            String aboutText,
            String instagram,
            String facebookUrl,
            /** Logo: URL o data URI. Vacío = usar el logo por defecto. */
            @Size(max = 5_000_000) String logoUrl,
            @Pattern(regexp = "^$|^(circle|square|rectangle)$", message = "Forma de logo desconocida")
            String logoShape,
            @Size(max = 2000) String whatsappIntro,
            @Size(max = 2000) String whatsappClosing,
            @Size(max = 500) String storeAddress,
            @Size(max = 8000) String helpText,
            @Size(max = 20000) String faqText
    ) {}

    /**
     * Diseño de página + color de marca (ver PLAN_SAAS.md Fase 10) — mismo
     * nivel de permiso que identidad/logo. Ambos campos son opcionales:
     * vacío/null = volver al valor por defecto del rubro (`layout`) o a la
     * paleta con nombre de siempre (`brandColor`).
     */
    public record AppearanceRequest(
            @Pattern(regexp = "^$|^(classic|minimal|boutique|curva|grid|mercado)$", message = "Diseño desconocido")
            String layout,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String brandColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String headerColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String footerColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String textColor,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Color inválido (formato #rrggbb)")
            String pageBackgroundColor
    ) {}

    /** Medios de pago. Editable por el admin normal de la tienda. */
    public record PaymentsSettingsRequest(
            Boolean paymentTransferEnabled,
            @Size(max = 200) String paymentTransferAlias,
            Boolean paymentQrTransferEnabled,
            @Size(max = 5_000_000) String paymentQrTransferImage,
            Boolean paymentQrCardEnabled,
            @Size(max = 5_000_000) String paymentQrCardImage,
            @Size(max = 1000) String paymentCardLink,
            Boolean paymentCashEnabled
    ) {}

    public record SettingsResponse(
            String storeName,
            String whatsappNumber,
            String aboutText,
            String instagram,
            String facebookUrl,
            String logoUrl,
            String logoShape,
            String theme,
            String layout,
            String brandColor,
            String headerColor,
            String footerColor,
            String textColor,
            String pageBackgroundColor,
            String whatsappIntro,
            String whatsappClosing,
            String storeAddress,
            String helpText,
            String faqText,
            boolean paymentTransferEnabled,
            String paymentTransferAlias,
            boolean paymentQrTransferEnabled,
            String paymentQrTransferImage,
            boolean paymentQrCardEnabled,
            String paymentQrCardImage,
            String paymentCardLink,
            boolean paymentCashEnabled,
            /**
             * Cuenta de Cloudinary para subir imágenes desde el panel. Sólo lectura acá
             * (se necesitan en cualquier sesión de admin para poder subir fotos); se
             * editan aparte, en `/api/admin/settings/cloudinary` (sólo superadmin).
             */
            String cloudinaryCloudName,
            String cloudinaryUploadPreset,
            /**
             * Módulos habilitados por el plan del tenant (ver
             * {@code com.saasweb.core.plan.Modules}). `false` = el panel/
             * checkout no debe mostrar esa opción, sin importar qué otro
             * dato haya cargado.
             */
            boolean socialShareEnabled,
            /**
             * true sólo si el módulo `MERCADOPAGO` está habilitado en el
             * plan Y el tenant activó el checkout Y ya cargó su Access
             * Token — recién ahí tiene sentido ofrecer "Pagar con Mercado
             * Pago" en el carrito. El Access Token en sí NUNCA viaja acá
             * (ver `MercadoPagoConfigResponse`, mismo criterio que SMTP).
             */
            boolean mercadoPagoAvailable,
            /**
             * Módulo `POS` habilitado en el plan (Fase 14) — el panel usa
             * esto para decidir si mostrar "Venta en el local"/el punto de
             * venta de kiosco en el menú.
             */
            boolean posEnabled,
            /**
             * Módulo `ECOMMERCE_SITE` habilitado en el plan (Fase 14) — un
             * tenant sin esto no tiene sitio público (`TenantResolutionFilter`
             * ya bloquea las rutas públicas del lado del backend); el panel
             * usa este flag para no mostrar pantallas de ecommerce (Catálogo
             * online, Apariencia, Carrusel, etc.) que no aplican.
             */
            boolean ecommerceSiteEnabled,
            /**
             * true sólo si el módulo `ARCA_INVOICING` está habilitado en el
             * plan Y el tenant cargó CUIT + certificado + punto de venta
             * (ver `ArcaInvoiceService#isAvailable`). El certificado/clave
             * NUNCA viajan acá (ver `ArcaConfigResponse`).
             */
            boolean arcaAvailable,
            /** "TICKET_INTERNO" | "FACTURA_ARCA" — qué ofrece por defecto el punto de venta. */
            String invoiceMode,
            /**
             * "MONOTRIBUTO" | "EXENTO" | "RESPONSABLE_INSCRIPTO" — el panel la
             * usa sólo para decidir si mostrar el campo de CUIT del comprador
             * en el punto de venta (habilita Factura A en vez de B).
             */
            String arcaCondicionIva
    ) {
        public static SettingsResponse from(SiteSettings s, boolean socialShareEnabled, boolean mercadoPagoAvailable,
                                             boolean posEnabled, boolean ecommerceSiteEnabled,
                                             boolean arcaAvailable, String invoiceMode) {
            return new SettingsResponse(
                    s.getStoreName(), s.getWhatsappNumber(), s.getAboutText(),
                    s.getInstagram(), s.getFacebookUrl(), s.getLogoUrl(),
                    s.getLogoShape() != null ? s.getLogoShape() : "circle",
                    s.getTheme() != null ? s.getTheme() : "default",
                    s.getLayout() != null ? s.getLayout() : "classic",
                    s.getBrandColor(),
                    s.getHeaderColor(), s.getFooterColor(), s.getTextColor(), s.getPageBackgroundColor(),
                    s.getWhatsappIntro(), s.getWhatsappClosing(), s.getStoreAddress(),
                    s.getHelpText(), s.getFaqText(),
                    s.isPaymentTransferEnabled(), s.getPaymentTransferAlias(),
                    s.isPaymentQrTransferEnabled(), s.getPaymentQrTransferImage(),
                    s.isPaymentQrCardEnabled(), s.getPaymentQrCardImage(),
                    s.getPaymentCardLink(), s.isPaymentCashEnabled(),
                    s.getCloudinaryCloudName(), s.getCloudinaryUploadPreset(),
                    socialShareEnabled, mercadoPagoAvailable, posEnabled, ecommerceSiteEnabled,
                    arcaAvailable, invoiceMode, s.getArcaCondicionIva());
        }
    }

    /**
     * Credenciales de Mercado Pago del tenant (Fase 13) — editable por
     * `PAYMENTS_MANAGE` (es SU cuenta, no una compartida de plataforma).
     * `accessToken` en blanco = no tocar el que ya está guardado (mismo
     * criterio que `MailConfigRequest.password`).
     */
    public record MercadoPagoConfigRequest(
            Boolean mpEnabled,
            @Size(max = 300) String accessToken,
            @Size(max = 300) String publicKey
    ) {}

    /** {@code accessToken} nunca se devuelve: sólo si hay uno guardado (accessTokenSet). */
    public record MercadoPagoConfigResponse(boolean mpEnabled, boolean accessTokenSet, String publicKey) {
        public static MercadoPagoConfigResponse from(SiteSettings s) {
            return new MercadoPagoConfigResponse(
                    s.isMpEnabled(),
                    s.getMpAccessToken() != null && !s.getMpAccessToken().isBlank(),
                    s.getMpPublicKey());
        }
    }

    /**
     * Credenciales de ARCA del tenant (Fase 14) — editable por
     * `PAYMENTS_MANAGE` (es SU CUIT, no uno compartido de plataforma).
     * `certificadoPem`/`clavePrivadaPem` en blanco = no tocar los ya
     * guardados (mismo criterio que `MercadoPagoConfigRequest.accessToken`).
     */
    public record ArcaConfigRequest(
            Boolean arcaEnabled,
            Boolean arcaModoPrueba,
            @Size(max = 20) String cuit,
            Integer puntoVenta,
            @Size(max = 40) String condicionIva,
            @Size(max = 8000) String certificadoPem,
            @Size(max = 8000) String clavePrivadaPem,
            @Pattern(regexp = "^(TICKET_INTERNO|FACTURA_ARCA)$", message = "Modo de comprobante desconocido")
            String invoiceMode
    ) {}

    /** El certificado/clave nunca se devuelven: sólo si hay uno guardado (ver `*Set`). */
    public record ArcaConfigResponse(
            boolean arcaEnabled, boolean arcaModoPrueba, String cuit, Integer puntoVenta,
            String condicionIva, boolean certificadoSet, boolean clavePrivadaSet, String invoiceMode
    ) {
        public static ArcaConfigResponse from(SiteSettings s) {
            return new ArcaConfigResponse(
                    s.isArcaEnabled(), s.isArcaModoPrueba(), s.getArcaCuit(), s.getArcaPuntoVenta(),
                    s.getArcaCondicionIva(),
                    s.getArcaCertificadoPem() != null && !s.getArcaCertificadoPem().isBlank(),
                    s.getArcaClavePrivadaPem() != null && !s.getArcaClavePrivadaPem().isBlank(),
                    s.getInvoiceMode());
        }
    }

    /** Alerta de stock bajo por mail (ver `LowStockAlertScheduler`) — la edita el admin normal de la tienda. */
    public record StockAlertSettingsRequest(Boolean lowStockAlertEnabled, @Email String lowStockAlertEmail) {}

    public record StockAlertSettingsResponse(boolean lowStockAlertEnabled, String lowStockAlertEmail) {
        public static StockAlertSettingsResponse from(SiteSettings s) {
            return new StockAlertSettingsResponse(s.isLowStockAlertEnabled(), s.getLowStockAlertEmail());
        }
    }

    /** Config de Cloudinary: sólo la puede ver/editar un superadmin. */
    public record CloudinaryConfigRequest(
            @Size(max = 200) String cloudName,
            @Size(max = 200) String uploadPreset
    ) {}

    public record CloudinaryConfigResponse(String cloudName, String uploadPreset) {
        public static CloudinaryConfigResponse from(SiteSettings s) {
            return new CloudinaryConfigResponse(s.getCloudinaryCloudName(), s.getCloudinaryUploadPreset());
        }
    }

    /**
     * Config de SMTP (recuperación de cuenta por mail): sólo la puede ver/editar
     * un superadmin. {@code password} vacío/null = no cambiar la que ya está
     * guardada (mismo patrón que el cambio de contraseña de un AdminUser).
     */
    public record MailConfigRequest(
            @Size(max = 300) String host,
            Integer port,
            @Size(max = 300) String username,
            @Size(max = 500) String password,
            @Size(max = 300) String fromEmail,
            @Size(max = 200) String fromName
    ) {}

    /** {@code password} nunca se devuelve: sólo si hay una guardada (passwordSet). */
    public record MailConfigResponse(
            String host, Integer port, String username,
            boolean passwordSet, String fromEmail, String fromName
    ) {
        public static MailConfigResponse from(SiteSettings s) {
            return new MailConfigResponse(
                    s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUsername(),
                    s.getSmtpPassword() != null && !s.getSmtpPassword().isBlank(),
                    s.getSmtpFromEmail(), s.getSmtpFromName());
        }
    }
}
