package com.estilospequenos.dto;

import com.estilospequenos.model.Coupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class CouponDtos {

    private CouponDtos() {}

    /** Alta / edición. Para generar códigos en lote, mandar `count` > 1 y (opcional) `codePrefix`. */
    public record CouponRequest(
            /** Código explícito (para 1 cupón). Si se genera en lote, se ignora. */
            @Size(max = 40) String code,
            /** Cantidad de códigos a generar (1 = usar `code` o generar uno). */
            @Min(1) Integer count,
            @Size(max = 20) String codePrefix,
            @NotNull Coupon.Kind kind,
            @NotNull @DecimalMin("0.0") BigDecimal value,
            BigDecimal minAmount,
            @Min(1) Integer maxUses,
            LocalDate expiresAt,
            Boolean enabled,
            Boolean stackable,
            @Size(max = 200) String label
    ) {}

    public record EnabledPatch(@NotNull Boolean enabled) {}

    public record CouponResponse(
            String id, String code, Coupon.Kind kind, BigDecimal value, BigDecimal minAmount,
            Integer maxUses, int usedCount, LocalDate expiresAt, boolean enabled, boolean stackable,
            String label, Instant createdAt, String status
    ) {
        public static CouponResponse from(Coupon c) {
            String status = !c.isEnabled() ? "DESHABILITADO"
                    : c.isExpired() ? "VENCIDO"
                    : c.isExhausted() ? "AGOTADO"
                    : "ACTIVO";
            return new CouponResponse(c.getId(), c.getCode(), c.getKind(), c.getValue(), c.getMinAmount(),
                    c.getMaxUses(), c.getUsedCount(), c.getExpiresAt(), c.isEnabled(), c.isStackable(),
                    c.getLabel(), c.getCreatedAt(), status);
        }
    }

    /** Resultado de validar un código en el carrito (no lo consume). */
    public record CouponCheckResponse(
            String code, Coupon.Kind kind, BigDecimal value, BigDecimal discountAmount,
            boolean stackable, String label
    ) {}
}
