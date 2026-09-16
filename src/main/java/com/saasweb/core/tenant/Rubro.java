package com.saasweb.core.tenant;

/**
 * Rubro de un tenant: determina qué plantilla de parametría/productos de
 * ejemplo se siembra al crearlo (ver {@code TenantProvisioningService}) y
 * el theme visual + layout por defecto. Ver PLAN_SAAS.md Fase 9 — hoy son 3
 * rubros fijos en código porque no hay todavía un catálogo de rubros
 * gestionable desde el panel; agregar uno nuevo es un valor de enum + su
 * seeder.
 */
public enum Rubro {
    ROPA("Indumentaria", "default", "classic", "👕", "#f97316"),
    FERRETERIA("Ferretería", "ferreteria", "classic", "🔧", "#a85f27"),
    REPUESTOS("Repuestos de autos", "repuestos", "classic", "⚙️", "#35586b");

    private final String label;
    private final String defaultTheme;
    private final String defaultLayout;
    private final String logoEmoji;
    private final String logoColor;

    Rubro(String label, String defaultTheme, String defaultLayout, String logoEmoji, String logoColor) {
        this.label = label;
        this.defaultTheme = defaultTheme;
        this.defaultLayout = defaultLayout;
        this.logoEmoji = logoEmoji;
        this.logoColor = logoColor;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    /**
     * Layout de página por defecto (ver PLAN_SAAS.md Fase 10) — eje
     * independiente de {@code defaultTheme}. Hoy sólo existe `"classic"`;
     * el campo es la base para poder ofrecer más de un layout por rubro sin
     * redeploy el día que haya un segundo construido de verdad.
     */
    public String getDefaultLayout() {
        return defaultLayout;
    }

    /** Emoji + color usados para el logo genérico de un tenant nuevo (ver {@code TenantProvisioningService}). */
    public String getLogoEmoji() {
        return logoEmoji;
    }

    public String getLogoColor() {
        return logoColor;
    }
}
