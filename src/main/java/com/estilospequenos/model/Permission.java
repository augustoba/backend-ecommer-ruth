package com.estilospequenos.model;

/**
 * Permisos ("objetos") que se le asignan a un {@link Role}. Cada uno habilita
 * una parte del panel. El rol "Superadmin" (system) los tiene todos siempre.
 */
public enum Permission {
    /** Ver el listado de productos. */
    PRODUCTS_VIEW("Ver productos"),
    /** Crear, editar, duplicar y archivar productos. */
    PRODUCTS_MANAGE("Gestionar productos"),
    /** Ver los pedidos de WhatsApp. */
    ORDERS_VIEW("Ver pedidos"),
    /** Confirmar / cancelar pedidos (descuenta stock). */
    ORDERS_MANAGE("Gestionar pedidos"),
    /** Registrar ventas en el local (punto de venta). */
    POS_USE("Registrar ventas en el local"),
    /** Registrar cambios de prenda en el local. */
    EXCHANGES_USE("Registrar cambios de prenda"),
    /** Ver el cierre de caja del día. */
    CASH_REGISTER_VIEW("Ver la caja (cierre del día)"),
    /** Abrir/cerrar el propio turno y ver su caja. */
    SHIFTS_MANAGE("Turnos"),
    /** Editar parametrías (clasificación de prendas). */
    PARAMS_MANAGE("Parametrías"),
    /** Editar escalas de talle. */
    SIZE_SCALES_MANAGE("Escalas de talle"),
    /** Editar proveedores. */
    SUPPLIERS_MANAGE("Proveedores"),
    /** Editar descuentos automáticos. */
    DISCOUNTS_MANAGE("Descuentos"),
    /** Editar cupones. */
    COUPONS_MANAGE("Cupones"),
    /** Configurar y correr campañas de marketing por email. */
    MARKETING_MANAGE("Marketing"),
    /** Ver métricas de ventas. */
    METRICS_VIEW("Métricas"),
    /** Editar el carrusel de la home. */
    CAROUSEL_MANAGE("Carrusel"),
    /** Editar medios de pago (alias, QRs, links). */
    PAYMENTS_MANAGE("Medios de pago"),
    /** Configuración de plataforma: identidad, logo, WhatsApp, redes, textos, servicio de mail. Sólo superadmin. */
    PLATFORM_SETTINGS_MANAGE("Configuración de plataforma"),
    /** Crear usuarios y roles del panel. */
    USERS_MANAGE("Usuarios y roles");

    private final String label;

    Permission(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
