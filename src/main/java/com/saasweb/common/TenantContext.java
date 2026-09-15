package com.saasweb.common;

/**
 * Tenant de la request actual. Lo setea {@code TenantResolutionFilter} al
 * principio de cada request y lo limpia al final (evita que un thread
 * reusado por el pool arrastre el tenant de la request anterior).
 *
 * <p>Hoy hay un único tenant posible (ver PLAN_SAAS.md), así que nada lee
 * todavía este valor para filtrar datos — existe para que el día que se
 * agregue `tenant_id` a las entidades, el resto del código ya tenga de
 * dónde sacar el tenant actual sin tener que volver a tocar el pipeline de
 * requests.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
