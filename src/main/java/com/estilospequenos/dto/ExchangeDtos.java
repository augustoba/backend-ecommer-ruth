package com.estilospequenos.dto;

import com.estilospequenos.model.Exchange;
import com.estilospequenos.model.ExchangeLine;
import com.estilospequenos.model.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ExchangeDtos {

    private ExchangeDtos() {}

    public record ExchangeItem(
            @NotBlank String productId,
            @NotBlank String size,
            @Min(1) @Max(99) int quantity
    ) {}

    public record CreateExchangeRequest(
            @Size(max = 120) String customerName,
            /** Prendas que el cliente devuelve (vuelven al stock). */
            @NotEmpty List<ExchangeItem> returned,
            /** Prendas que se lleva (salen del stock). */
            @NotEmpty List<ExchangeItem> taken,
            /** Medio de pago de la diferencia (si la hay). */
            PaymentMethod paymentMethod,
            @Size(max = 500) String note
    ) {}

    public record ExchangeLineResponse(
            String kind, String productId, String productName, String size,
            int quantity, BigDecimal unitPrice
    ) {
        static ExchangeLineResponse from(ExchangeLine l) {
            return new ExchangeLineResponse(l.getKind().name(), l.getProductId(), l.getProductName(),
                    l.getSize(), l.getQuantity(), l.getUnitPrice());
        }
    }

    public record ExchangeResponse(
            String id, String code, String customerName,
            BigDecimal returnedTotal, BigDecimal takenTotal, BigDecimal difference,
            PaymentMethod paymentMethod, String note, Instant createdAt, String processedByName,
            List<ExchangeLineResponse> lines
    ) {
        public static ExchangeResponse from(Exchange e) {
            return new ExchangeResponse(
                    e.getId(), e.getCode(), e.getCustomerName(),
                    e.getReturnedTotal(), e.getTakenTotal(), e.getDifference(),
                    e.getPaymentMethod(), e.getNote(), e.getCreatedAt(), e.getProcessedByName(),
                    e.getLines().stream().map(ExchangeLineResponse::from).toList());
        }
    }
}
