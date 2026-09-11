package com.estilospequenos.dto;

import com.estilospequenos.model.MarketingConfig;
import com.estilospequenos.model.MarketingSend;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class MarketingDtos {

    private MarketingDtos() {}

    public record MarketingConfigRequest(
            @NotNull Boolean enabled,
            @Min(1) @Max(100) int discountPercent,
            @Min(1) int inactivityDays,
            @NotNull @DecimalMin("0.0") BigDecimal spendThreshold,
            @Min(1) @Max(1000) int dailyEmailCap,
            @Min(1) int couponValidityDays,
            @Min(0) int cooldownDays
    ) {}

    public record MarketingConfigResponse(
            boolean enabled, int discountPercent, int inactivityDays, BigDecimal spendThreshold,
            int dailyEmailCap, int couponValidityDays, int cooldownDays
    ) {
        public static MarketingConfigResponse from(MarketingConfig c) {
            return new MarketingConfigResponse(c.isEnabled(), c.getDiscountPercent(), c.getInactivityDays(),
                    c.getSpendThreshold(), c.getDailyEmailCap(), c.getCouponValidityDays(), c.getCooldownDays());
        }
    }

    /** Un candidato a recibir campaña (para la vista previa). */
    public record CandidatePreview(
            String email, MarketingSend.Reason reason, BigDecimal lifetimeSpend, Instant lastOrderAt
    ) {}

    public record PreviewResult(
            int totalQualifying, int alreadySentToday, int remainingCapToday,
            int excludedByCooldown, int willBeEmailedToday, java.util.List<CandidatePreview> sample
    ) {}

    public record RunResult(int attempted, int sent, int failed, int skippedCap) {}

    public record MarketingSendResponse(
            String id, String email, MarketingSend.Reason reason, String couponCode,
            Instant sentAt, MarketingSend.Status status, String errorMessage,
            BigDecimal lifetimeSpendSnapshot, Instant lastOrderAtSnapshot
    ) {
        public static MarketingSendResponse from(MarketingSend s) {
            return new MarketingSendResponse(s.getId(), s.getEmail(), s.getReason(), s.getCouponCode(),
                    s.getSentAt(), s.getStatus(), s.getErrorMessage(),
                    s.getLifetimeSpendSnapshot(), s.getLastOrderAtSnapshot());
        }
    }
}
