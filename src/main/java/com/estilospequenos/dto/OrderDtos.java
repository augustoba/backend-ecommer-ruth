package com.estilospequenos.dto;

import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    // --- entrada ---

    public record CartItem(
            @NotBlank String productId,
            @NotBlank String size,
            @Min(1) int quantity
    ) {}

    public record CreateOrderRequest(
            String customerName,
            @NotEmpty List<CartItem> items
    ) {}

    public record LineAcceptance(@NotBlank String lineId, @NotNull Boolean accepted) {}

    public record LinesRequest(@NotEmpty List<LineAcceptance> lines) {}

    // --- salida ---

    public record OrderLineResponse(
            String id, String productId, String productName, String size,
            int quantity, BigDecimal unitPrice, boolean accepted
    ) {
        static OrderLineResponse from(OrderLine l) {
            return new OrderLineResponse(l.getId(), l.getProductId(), l.getProductName(), l.getSize(),
                    l.getQuantity(), l.getUnitPrice(), l.isAccepted());
        }
    }

    public record OrderResponse(
            String id, String code, String customerName,
            BigDecimal subtotal, int discountPercent, BigDecimal discountAmount, BigDecimal total,
            OrderStatus status, Instant createdAt, Instant processedAt,
            List<OrderLineResponse> lines
    ) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(), o.getCode(), o.getCustomerName(),
                    o.getSubtotal(), o.getDiscountPercent(), o.getDiscountAmount(), o.getTotal(),
                    o.getStatus(), o.getCreatedAt(), o.getProcessedAt(),
                    o.getLines().stream().map(OrderLineResponse::from).toList());
        }
    }
}
