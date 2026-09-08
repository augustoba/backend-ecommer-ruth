package com.estilospequenos.dto;

import com.estilospequenos.model.Discount;
import com.estilospequenos.model.PaymentMethod;
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
            Boolean stackable,
            String label,
            String detail,
            LocalDate startsAt,
            LocalDate endsAt,
            BigDecimal minAmount,
            String groupId,
            String optionId,
            List<PaymentMethod> paymentMethods
    ) {}

    public record DiscountResponse(
            String id, Discount.Kind kind, int discountPercent, boolean enabled, boolean stackable,
            String label, String detail, LocalDate startsAt, LocalDate endsAt,
            /** ACTIVO | PROGRAMADO | VENCIDO | DESHABILITADO — con la fecha del servidor. */
            String status,
            BigDecimal minAmount, String groupId, String optionId, List<PaymentMethod> paymentMethods
    ) {
        public static DiscountResponse from(Discount d) {
            return new DiscountResponse(d.getId(), d.getKind(), d.getDiscountPercent(), d.isEnabled(),
                    d.isStackable(), d.getLabel(), d.getDetail(), d.getStartsAt(), d.getEndsAt(), status(d),
                    d.getMinAmount(), d.getGroupId(), d.getOptionId(),
                    d.paymentMethodSet().stream().toList());
        }

        private static String status(Discount d) {
            if (!d.isEnabled()) return "DESHABILITADO";
            LocalDate today = LocalDate.now();
            if (d.getStartsAt() != null && today.isBefore(d.getStartsAt())) return "PROGRAMADO";
            if (d.getEndsAt() != null && today.isAfter(d.getEndsAt())) return "VENCIDO";
            return "ACTIVO";
        }
    }

    /** Reglas de descuento para el catálogo público (calcula el preview del carrito). */
    public record PublicDiscounts(List<DiscountResponse> discounts) {}

    /** Detalle de un descuento aplicado a un carrito. */
    public record BreakdownLine(String label, BigDecimal amount, String detail) {}

    /** "Envío gratis" (informativo, no descuenta plata). */
    public record FreeShipping(String label, String detail) {}

    public record CartDiscountResult(
            int discountPercent,
            BigDecimal discountAmount,
            List<BreakdownLine> breakdown,
            FreeShipping freeShipping
    ) {}
}
