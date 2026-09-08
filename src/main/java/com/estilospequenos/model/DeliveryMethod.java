package com.estilospequenos.model;

/** Cómo recibe el pedido el cliente. */
public enum DeliveryMethod {
    /** Retira en el local. */
    PICKUP,
    /** Envío a domicilio (el costo se coordina por WhatsApp). */
    SHIPPING
}
