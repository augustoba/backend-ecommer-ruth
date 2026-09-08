package com.estilospequenos.dto;

import com.estilospequenos.model.Discount;
import com.estilospequenos.model.DiscountConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DiscountDtos {

    private DiscountDtos() {}

    public record DiscountRequest(
            @NotNull Discount.Kind kind,
            @Min(0) @Max(100) int discountPercent,
            Boolean enabled,
            String label,
            LocalDate startsAt,
            LocalDate endsAt,
            BigDecimal minAmount,
            String groupId,
            String optionId
    ) {}

    public record DiscountResponse(
            String id, Discount.Kind kind, int discountPercent, boolean enabled,
            String label, LocalDate startsAt, LocalDate endsAt,
            /** ACTIVO | PROGRAMADO | VENCIDO | DESHABILITADO — con la fecha del servidor. */
            String status,
            BigDecimal minAmount, String groupId, String optionId
    ) {
        public static DiscountResponse from(Discount d) {
            return new DiscountResponse(d.getId(), d.getKind(), d.getDiscountPercent(), d.isEnabled(),
                    d.getLabel(), d.getStartsAt(), d.getEndsAt(), status(d),
                    d.getMinAmount(), d.getGroupId(), d.getOptionId());
        }

        private static String status(Discount d) {
            if (!d.isEnabled()) return "DESHABILITADO";
            LocalDate today = LocalDate.now();
            if (d.getStartsAt() != null && today.isBefore(d.getStartsAt())) return "PROGRAMADO";
            if (d.getEndsAt() != null && today.isAfter(d.getEndsAt())) return "VENCIDO";
            return "ACTIVO";
        }
    }

    public record ConfigRequest(@NotNull DiscountConfig.CombineMode combineMode) {}

    public record ConfigResponse(DiscountConfig.CombineMode combineMode) {
        public static ConfigResponse from(DiscountConfig c) {
            return new ConfigResponse(c.getCombineMode());
        }
    }

    /** Reglas de descuento para el catálogo público (calcula el preview del carrito). */
    public record PublicDiscounts(
            List<DiscountResponse> discounts,
            DiscountConfig.CombineMode combineMode
    ) {}

    /** Detalle de un descuento aplicado a un carrito. */
    public record BreakdownLine(String label, BigDecimal amount) {}

    public record CartDiscountResult(
            int discountPercent,
            BigDecimal discountAmount,
            List<BreakdownLine> breakdown
    ) {}
}
