package com.saasweb.core.arca;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public final class CreditNoteDtos {

    private CreditNoteDtos() {}

    public record CreditNoteRequest(@NotNull @Positive BigDecimal amount, String reason) {}

    public record CreditNoteResponse(
            String id, String orderId, BigDecimal amount, String reason, String type,
            String cae, String caeVencimiento, Long number, Integer puntoVenta, String qrUrl,
            String error, String createdByName, Instant createdAt
    ) {
        public static CreditNoteResponse from(CreditNote cn) {
            return new CreditNoteResponse(cn.getId(), cn.getOrderId(), cn.getAmount(), cn.getReason(), cn.getType(),
                    cn.getCae(), cn.getCaeVencimiento(), cn.getNumber(), cn.getPuntoVenta(), cn.getQrUrl(),
                    cn.getError(), cn.getCreatedByName(), cn.getCreatedAt());
        }
    }
}
