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
}
