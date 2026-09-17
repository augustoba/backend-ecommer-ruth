package com.saasweb.core.product;

/** Por qué cambió el stock de un talle — ver {@link StockMovement}. */
public enum StockMovementReason {
    /** Confirmación de un pedido (venta). */
    VENTA,
    /** Cambio de prenda: lo que el cliente devuelve. */
    CAMBIO_DEVUELTA,
    /** Cambio de prenda: lo que el cliente se lleva. */
    CAMBIO_LLEVADA,
    /** Ajuste manual desde el panel (rotura, robo, conteo físico, etc.). */
    AJUSTE_MANUAL,
    /** Entrada de mercadería comprada a un proveedor. */
    ENTRADA_COMPRA,
    /** Stock inicial cargado al crear el producto. */
    ALTA_INICIAL
}
