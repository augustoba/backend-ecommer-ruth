package com.estilospequenos.model;

/**
 * Estado de una línea de pedido — permite entregar/cancelar parte de un
 * pedido en vez de todo junto (ej: había 3 productos, 2 tenían stock y se
 * entregaron, el tercero no llegó y se cancela solo). Ver
 * {@link com.estilospequenos.service.OrderService#confirmLines} /
 * {@link com.estilospequenos.service.OrderService#cancelLines}.
 */
public enum OrderLineStatus {
    /** Todavía no se entregó ni se canceló — el pedido sigue esperando esta línea. */
    PENDIENTE,
    /** Se descontó stock y se le entregó al cliente. */
    ENTREGADA,
    /** No se va a entregar (sin stock, el cliente desistió de ese ítem, etc). */
    CANCELADA
}
