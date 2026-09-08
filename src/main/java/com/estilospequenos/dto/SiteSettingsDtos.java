package com.estilospequenos.dto;

import com.estilospequenos.model.SiteSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class SiteSettingsDtos {

    private SiteSettingsDtos() {}

    public record SettingsRequest(
            @NotBlank String storeName,
            @NotBlank
            @Pattern(regexp = "\\d{8,15}", message = "Solo números, sin +, espacios ni 15 (8 a 15 dígitos)")
            String whatsappNumber,
            String aboutText,
            String instagram,
            String facebookUrl
    ) {}

    public record SettingsResponse(
            String storeName,
            String whatsappNumber,
            String aboutText,
            String instagram,
            String facebookUrl
    ) {
        public static SettingsResponse from(SiteSettings s) {
            return new SettingsResponse(
                    s.getStoreName(), s.getWhatsappNumber(), s.getAboutText(),
                    s.getInstagram(), s.getFacebookUrl());
        }
    }
}
