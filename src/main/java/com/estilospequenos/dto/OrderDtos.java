package com.estilospequenos.dto;

import com.estilospequenos.model.DeliveryMethod;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderLineStatus;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.PaymentMethod;
import com.estilospequenos.model.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
            /** Opcional: para la base de clientes y campañas de marketing. */
            @Size(max = 200) String customerEmail,
            @NotEmpty List<CartItem> items,
            /** null = PICKUP (compat con clientes viejos). */
            DeliveryMethod deliveryMethod,
            @Size(max = 500) String shippingAddress,
            @Size(max = 500) String shippingReference,
            Double shippingLat,
            Double shippingLng,
            PaymentMethod paymentMethod,
            /** Código de cupón escrito en el carrito (opcional). */
            @Size(max = 40) String couponCode
    ) {}

    /** IDs de las líneas a entregar/cancelar (entrega parcial de un pedido pendiente). */
    public record LineIdsRequest(@NotEmpty List<String> lineIds) {}

    /** Agrega un ítem a un pedido todavía pendiente. */
    public record AddLineRequest(
            @NotBlank String productId,
            @NotBlank String size,
            @Min(1) @Max(999) int quantity
    ) {}

    /** Cambia la cantidad de una línea todavía pendiente. */
    public record LineQuantityRequest(@Min(1) @Max(999) int quantity) {}

    // --- salida ---

    public record OrderLineResponse(
            String id, String productId, String productName, String size,
            int quantity, BigDecimal unitPrice, boolean accepted, OrderLineStatus status
    ) {
        static OrderLineResponse from(OrderLine l) {
            return new OrderLineResponse(l.getId(), l.getProductId(), l.getProductName(), l.getSize(),
                    l.getQuantity(), l.getUnitPrice(), l.isAccepted(), l.getStatus());
        }
    }

    /** Vista pública de un pedido (consulta "mis pedidos" con código + nombre). Sin datos internos. */
    public record PublicOrderResponse(
            String code, String customerName, OrderStatus status,
            Instant createdAt, Instant processedAt,
            DeliveryMethod deliveryMethod, BigDecimal subtotal, int discountPercent,
            BigDecimal discountAmount, BigDecimal total,
            PaymentMethod paymentMethod, PaymentStatus paymentStatus,
            /** Sólo si el pago todavía está PENDING — para poder reintentar. */
            String mpCheckoutUrl,
            List<PublicOrderLine> items
    ) {
        public record PublicOrderLine(String productName, String size, int quantity, BigDecimal unitPrice) {}

        public static PublicOrderResponse from(Order o) {
            List<PublicOrderLine> items = o.getLines().stream()
                    .map(l -> new PublicOrderLine(l.getProductName(), l.getSize(), l.getQuantity(), l.getUnitPrice()))
                    .toList();
            boolean canRetry = o.getPaymentStatus() == PaymentStatus.PENDING;
            return new PublicOrderResponse(
                    o.getCode(), o.getCustomerName(), o.getStatus(),
                    o.getCreatedAt(), o.getProcessedAt(),
                    o.getDeliveryMethod(), o.getSubtotal(), o.getDiscountPercent(),
                    o.getDiscountAmount(), o.getTotal(),
                    o.getPaymentMethod(), o.getPaymentStatus(),
                    canRetry ? o.getMpCheckoutUrl() : null,
                    items);
        }
    }

    public record OrderResponse(
            String id, String code, String customerName, String customerEmail,
            BigDecimal subtotal, int discountPercent, BigDecimal discountAmount, BigDecimal total,
            OrderStatus status, com.estilospequenos.model.SaleChannel channel,
            Instant createdAt, Instant processedAt,
            DeliveryMethod deliveryMethod, String shippingAddress, String shippingReference,
            Double shippingLat, Double shippingLng, PaymentMethod paymentMethod,
            String freeShippingNote, String discountNote,
            String couponCode, BigDecimal couponDiscount,
            String createdByName, String confirmedByName,
            /** Sólo `paymentMethod = MERCADOPAGO`. */
            PaymentStatus paymentStatus,
            /** Link al checkout de Mercado Pago — el frontend redirige acá apenas se crea el pedido. */
            String mpCheckoutUrl,
            List<OrderLineResponse> lines
    ) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(), o.getCode(), o.getCustomerName(), o.getCustomerEmail(),
                    o.getSubtotal(), o.getDiscountPercent(), o.getDiscountAmount(), o.getTotal(),
                    o.getStatus(), o.getChannel(), o.getCreatedAt(), o.getProcessedAt(),
                    o.getDeliveryMethod(), o.getShippingAddress(), o.getShippingReference(),
                    o.getShippingLat(), o.getShippingLng(), o.getPaymentMethod(),
                    o.getFreeShippingNote(), o.getDiscountNote(),
                    o.getCouponCode(), o.getCouponDiscount(),
                    o.getCreatedByName(), o.getConfirmedByName(),
                    o.getPaymentStatus(), o.getMpCheckoutUrl(),
                    o.getLines().stream().map(OrderLineResponse::from).toList());
        }
    }
}
