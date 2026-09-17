package com.saasweb.core.order;

import com.saasweb.core.order.DeliveryMethod;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderLine;
import com.saasweb.core.order.OrderStatus;
import com.saasweb.core.order.PaymentMethod;
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
            @Size(max = 40) String couponCode,
            /** Venta presencial en efectivo: con cuánto pagó el cliente (opcional, sólo con CASH). */
            BigDecimal amountTendered,
            /** Referencia anotada a mano (nombre de quien transfirió, o número de ticket del posnet). Opcional. */
            @Size(max = 200) String paymentReference,
            /** CUIT del comprador (opcional) — sólo tiene efecto si la tienda es Responsable Inscripto ante ARCA: habilita Factura A en vez de B. */
            @Size(max = 20) String buyerCuit
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
            OrderStatus status, com.saasweb.core.order.SaleChannel channel,
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
            /** Mercado Pago aprobó el pago pero no se pudo confirmar el pedido solo — ver {@link Order#getPaymentIssueNote()}. null = sin problemas. */
            String paymentIssueNote,
            /** Venta presencial en efectivo: con cuánto pagó el cliente. null salvo CASH (y aun ahí es opcional). */
            BigDecimal amountTendered,
            /** Nombre de quien transfirió, o número de ticket del posnet, según el medio de pago. Opcional. */
            String paymentReference,
            /** "TICKET_INTERNO" | "FACTURA_A" | "FACTURA_B" | "FACTURA_C" — sólo se completa al confirmar una venta presencial (canal LOCAL). */
            String invoiceType,
            /** CUIT del comprador, si se cargó al cobrar (sólo tiene efecto en tiendas Responsable Inscripto). */
            String invoiceBuyerCuit,
            String invoiceCae,
            String invoiceCaeVencimiento,
            Long invoiceNumber,
            Integer invoicePuntoVenta,
            String invoiceQrUrl,
            /** ARCA rechazó la Factura C (o falló la conexión) — la venta quedó igual como ticket interno. null = sin problemas. */
            String invoiceError,
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
                    o.getPaymentStatus(), o.getMpCheckoutUrl(), o.getPaymentIssueNote(),
                    o.getAmountTendered(), o.getPaymentReference(),
                    o.getInvoiceType(), o.getInvoiceBuyerCuit(), o.getInvoiceCae(), o.getInvoiceCaeVencimiento(),
                    o.getInvoiceNumber(), o.getInvoicePuntoVenta(), o.getInvoiceQrUrl(), o.getInvoiceError(),
                    o.getLines().stream().map(OrderLineResponse::from).toList());
        }
    }
}
