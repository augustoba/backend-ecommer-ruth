package com.saasweb.core.tenant;

/**
 * Rubro de un tenant: determina qué plantilla de parametría/productos de
 * ejemplo se siembra al crearlo (ver {@code TenantProvisioningService}) y
 * el theme visual por defecto. Ver PLAN_SAAS.md Fase 9 — hoy son 3 rubros
 * fijos en código porque no hay todavía un catálogo de rubros gestionable
 * desde el panel; agregar uno nuevo es un valor de enum + su seeder.
 */
public enum Rubro {
    ROPA("Indumentaria", "default"),
    FERRETERIA("Ferretería", "ferreteria"),
    REPUESTOS("Repuestos de autos", "repuestos");

    private final String label;
    private final String defaultTheme;

    Rubro(String label, String defaultTheme) {
        this.label = label;
        this.defaultTheme = defaultTheme;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }
}
