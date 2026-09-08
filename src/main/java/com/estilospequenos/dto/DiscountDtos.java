package com.estilospequenos.dto;

import com.estilospequenos.model.Discount;
import com.estilospequenos.model.DiscountConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class DiscountDtos {

    private DiscountDtos() {}

    public record DiscountRequest(
            @NotNull Discount.Kind kind,
            @Min(0) @Max(100) int discountPercent,
            Boolean enabled,
            String label,
            BigDecimal minAmount,
            String groupId,
            String optionId
    ) {}

    public record DiscountResponse(
            String id, Discount.Kind kind, int discountPercent, boolean enabled,
            String label, BigDecimal minAmount, String groupId, String optionId
    ) {
        public static DiscountResponse from(Discount d) {
            return new DiscountResponse(d.getId(), d.getKind(), d.getDiscountPercent(), d.isEnabled(),
                    d.getLabel(), d.getMinAmount(), d.getGroupId(), d.getOptionId());
        }
    }

    public record ConfigRequest(@NotNull DiscountConfig.CombineMode combineMode) {}

    public record ConfigResponse(DiscountConfig.CombineMode combineMode) {
        public static ConfigResponse from(DiscountConfig c) {
            return new ConfigResponse(c.getCombineMode());
        }
    }

    /** Detalle de un descuento aplicado a un carrito. */
    public record BreakdownLine(String label, BigDecimal amount) {}

    public record CartDiscountResult(
            int discountPercent,
            BigDecimal discountAmount,
            List<BreakdownLine> breakdown
    ) {}
}
