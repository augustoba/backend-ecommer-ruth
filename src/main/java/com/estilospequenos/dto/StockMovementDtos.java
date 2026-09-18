package com.estilospequenos.dto;

import com.estilospequenos.model.StockMovement;
import com.estilospequenos.model.StockMovementReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public final class StockMovementDtos {

    private StockMovementDtos() {}

    public record PurchaseRequest(
            @NotBlank String size,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitCost,
            String supplierId
    ) {}

    public record MovementResponse(
            String id, String productId, String productName, String size, int quantityDelta,
            StockMovementReason reason, String note, String referenceId, BigDecimal unitCost,
            String createdByName, Instant createdAt
    ) {
        public static MovementResponse from(StockMovement m) {
            return new MovementResponse(m.getId(), m.getProductId(), m.getProductName(), m.getSize(),
                    m.getQuantityDelta(), m.getReason(), m.getNote(), m.getReferenceId(), m.getUnitCost(),
                    m.getCreatedByName(), m.getCreatedAt());
        }
    }
}
