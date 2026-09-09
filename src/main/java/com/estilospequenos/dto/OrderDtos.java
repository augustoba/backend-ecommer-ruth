package com.estilospequenos.dto;

import com.estilospequenos.model.DeliveryMethod;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    // --- entrada ---

    public record CartItem(
            @NotBlank String productId,
            @NotBlank String size,
            @Min(1) @Max(999) int quantity
    ) {}

    public record CreateOrderRequest(
            String customerName,
            @NotEmpty List<CartItem> items,
            /** null = PICKUP (compat con clientes viejos). */
            DeliveryMethod deliveryMethod,
            @Size(max = 500) String shippingAddress,
            @Size(max = 500) String shippingReference,
            Double shippingLat,
            Double shippingLng,
            PaymentMethod paymentMethod
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

    /** Vista pública de un pedido (consulta "mis pedidos" con código + nombre). Sin datos internos. */
    public record PublicOrderResponse(
            String code, String customerName, OrderStatus status,
            Instant createdAt, Instant processedAt,
            DeliveryMethod deliveryMethod, BigDecimal subtotal, int discountPercent,
            BigDecimal discountAmount, BigDecimal total,
            List<PublicOrderLine> items
    ) {
        public record PublicOrderLine(String productName, String size, int quantity, BigDecimal unitPrice) {}

        public static PublicOrderResponse from(Order o) {
            List<PublicOrderLine> items = o.getLines().stream()
                    .map(l -> new PublicOrderLine(l.getProductName(), l.getSize(), l.getQuantity(), l.getUnitPrice()))
                    .toList();
            return new PublicOrderResponse(
                    o.getCode(), o.getCustomerName(), o.getStatus(),
                    o.getCreatedAt(), o.getProcessedAt(),
                    o.getDeliveryMethod(), o.getSubtotal(), o.getDiscountPercent(),
                    o.getDiscountAmount(), o.getTotal(), items);
        }
    }

    public record OrderResponse(
            String id, String code, String customerName,
            BigDecimal subtotal, int discountPercent, BigDecimal discountAmount, BigDecimal total,
            OrderStatus status, Instant createdAt, Instant processedAt,
            DeliveryMethod deliveryMethod, String shippingAddress, String shippingReference,
            Double shippingLat, Double shippingLng, PaymentMethod paymentMethod,
            String freeShippingNote, String discountNote,
            List<OrderLineResponse> lines
    ) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(), o.getCode(), o.getCustomerName(),
                    o.getSubtotal(), o.getDiscountPercent(), o.getDiscountAmount(), o.getTotal(),
                    o.getStatus(), o.getCreatedAt(), o.getProcessedAt(),
                    o.getDeliveryMethod(), o.getShippingAddress(), o.getShippingReference(),
                    o.getShippingLat(), o.getShippingLng(), o.getPaymentMethod(),
                    o.getFreeShippingNote(), o.getDiscountNote(),
                    o.getLines().stream().map(OrderLineResponse::from).toList());
        }
    }
}
