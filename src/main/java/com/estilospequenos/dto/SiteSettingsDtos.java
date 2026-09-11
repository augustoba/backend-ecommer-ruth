package com.estilospequenos.dto;

import com.estilospequenos.model.SiteSettings;
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
            @Size(max = 2000) String whatsappIntro,
            @Size(max = 2000) String whatsappClosing,
            @Size(max = 500) String storeAddress,
            @Size(max = 8000) String helpText,
            @Size(max = 20000) String faqText
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
            boolean paymentCashEnabled
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
                    s.getPaymentCardLink(), s.isPaymentCashEnabled());
        }
    }
}
