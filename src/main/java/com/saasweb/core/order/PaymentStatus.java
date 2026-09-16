package com.saasweb.core.order;

/**
 * Estado del pago online (Mercado Pago, Fase 13) — separado de
 * {@link OrderStatus} a propósito: `OrderStatus` sigue siendo el flujo
 * manual de siempre (PENDIENTE/PROCESADO/CANCELADO, un humano confirma o
 * cancela desde el panel). `PaymentStatus` sólo existe para pedidos con
 * `paymentMethod = MERCADOPAGO` — null para cualquier otro medio de pago
 * (transferencia, efectivo, etc. no tienen validación online).
 */
public enum PaymentStatus {
    /** Se creó la preferencia de pago, todavía no llegó ninguna notificación de Mercado Pago. */
    PENDING,
    /** Mercado Pago confirmó el pago — dispara la confirmación automática del pedido (ver OrderService). */
    APPROVED,
    /** Mercado Pago rechazó/canceló el pago — el pedido se cancela solo (nunca se tocó el stock). */
    REJECTED
}
