package com.saasweb.core.order;

/** Cómo va a pagar el cliente (lo elige en el carrito, para que el dueño sepa qué mandar). */
public enum PaymentMethod {
    /** Transferencia por alias/CBU. */
    TRANSFER,
    /** QR de transferencia (el dueño manda la imagen). */
    QR_TRANSFER,
    /** QR o link de pago con tarjeta. */
    QR_CARD,
    /** Efectivo al recibir/retirar. */
    CASH,
    /** Pago online real con Mercado Pago (Fase 13, checkout redirect) — ver `core/payment/`. */
    MERCADOPAGO,
    /**
     * Tarjeta con posnet físico del local (venta presencial) — el sistema no
     * se integra con el posnet en sí, sólo se anota el número de ticket que
     * imprime al aprobar (ver {@code Order#getPaymentReference()}).
     */
    POSNET
}
