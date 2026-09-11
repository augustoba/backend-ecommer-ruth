package com.estilospequenos.dto;

import com.estilospequenos.model.SiteSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SiteSettingsDtos {

    private SiteSettingsDtos() {}

    public record SettingsRequest(
            @NotBlank String storeName,
            @NotBlank
            @Pattern(regexp = "\\d{8,15}", message = "Solo números, sin +, espacios ni 15 (8 a 15 dígitos)")
            String whatsappNumber,
            String aboutText,
            String instagram,
            String facebookUrl,
            /** Logo: URL o data URI. Vacío = usar el logo por defecto. */
            @Size(max = 5_000_000) String logoUrl,
            @Size(max = 2000) String whatsappIntro,
            @Size(max = 2000) String whatsappClosing,
            @Size(max = 500) String storeAddress,
            @Size(max = 8000) String helpText,
            @Size(max = 20000) String faqText,
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
            String cloudinaryUploadPreset
    ) {
        public static SettingsResponse from(SiteSettings s) {
            return new SettingsResponse(
                    s.getStoreName(), s.getWhatsappNumber(), s.getAboutText(),
                    s.getInstagram(), s.getFacebookUrl(), s.getLogoUrl(),
                    s.getWhatsappIntro(), s.getWhatsappClosing(), s.getStoreAddress(),
                    s.getHelpText(), s.getFaqText(),
                    s.isPaymentTransferEnabled(), s.getPaymentTransferAlias(),
                    s.isPaymentQrTransferEnabled(), s.getPaymentQrTransferImage(),
                    s.isPaymentQrCardEnabled(), s.getPaymentQrCardImage(),
                    s.getPaymentCardLink(), s.isPaymentCashEnabled(),
                    s.getCloudinaryCloudName(), s.getCloudinaryUploadPreset());
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
