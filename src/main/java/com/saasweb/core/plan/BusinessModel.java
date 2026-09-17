package com.saasweb.core.plan;

/**
 * Modelo de negocio de un plan (Fase 17) — determina qué módulos tiene
 * sentido ofrecerle: un plan "Punto de venta" no debería poder tildar
 * `MERCADOPAGO` (no hay carrito online), un plan "Ecommerce" no debería
 * poder tildar `POS` (no tiene sentido ofrecer el kiosco dedicado a una
 * tienda que ya vende por "Venta en el local"). Ver {@link Modules#compatibleWith}.
 */
public final class BusinessModel {

    private BusinessModel() {
    }

    public static final String ECOMMERCE = "ECOMMERCE";
    public static final String POS = "POS";
}
