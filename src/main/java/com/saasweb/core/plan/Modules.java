package com.saasweb.core.plan;

/**
 * Claves de módulo válidas para {@link Plan#getEnabledModules()} — sólo
 * documentan qué strings existen, no hay validación server-side de que
 * `enabledModules` sólo contenga claves conocidas (mismo criterio laxo que
 * el resto de ese campo, ver el javadoc de {@code Plan}).
 */
public final class Modules {

    private Modules() {
    }

    /**
     * Botón "Publicar en redes" del formulario/listado de productos
     * (comparte foto + texto vía la Web Share API del celular del admin —
     * no usa la API de Meta, no hay nada que configurar del lado del
     * backend además de este flag).
     */
    public static final String SOCIAL_SHARE = "SOCIAL_SHARE";

    /**
     * Checkout con pago online real vía Mercado Pago (Fase 13) — cada
     * tenant carga sus propias credenciales (ver
     * `SiteSettings.mpAccessToken`/`mpPublicKey`/`mpEnabled`), este flag
     * sólo habilita que la opción exista para ese plan.
     */
    public static final String MERCADOPAGO = "MERCADOPAGO";

    /**
     * El sitio público en sí (Fase 14) — sin este módulo, `TenantResolutionFilter`
     * bloquea las rutas públicas (mismo criterio que una tienda pausada):
     * el tenant sólo existe para el panel de administración, no tiene
     * vidriera. Pensado para negocios de venta puramente presencial
     * (kiosco, casa de repuestos) que contratan sólo el punto de venta, sin
     * página web.
     */
    public static final String ECOMMERCE_SITE = "ECOMMERCE_SITE";

    /**
     * Punto de venta presencial con código de barras — hoy (Fase 14,
     * arrancando) esto sigue viviendo en la misma pantalla "Venta en el
     * local" que ya existía sin gate; este módulo es el que, más adelante,
     * decide si un tenant ve esa pantalla (con o sin `ECOMMERCE_SITE`) o la
     * pantalla dedicada de kiosco, todavía sin construir.
     */
    public static final String POS = "POS";

    /**
     * Factura electrónica real vía ARCA/AFIP (Fase 14) — cada tenant carga
     * su propio CUIT + certificado (ver `SiteSettings.arca*`), este flag
     * sólo habilita que la opción exista para ese plan. Sin este módulo (o
     * sin credenciales cargadas), el punto de venta sólo puede emitir
     * ticket interno, no fiscal.
     */
    public static final String ARCA_INVOICING = "ARCA_INVOICING";
}
