package com.saasweb.config;

import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resuelve el tenant de la request y lo deja en {@link TenantContext} para
 * el resto del pipeline. Hoy (ver PLAN_SAAS.md) sólo existe un tenant por
 * deploy, así que resuelve siempre ese — no lee todavía el header `Host`.
 * Cuando haya multi-tenant real, este es el único lugar que cambia (mapear
 * `Host` → tenant); el resto del código ya va a estar leyendo de
 * {@code TenantContext} y no necesita tocarse.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    /**
     * Selector de tienda modo demo (ver PLAN_SAAS.md): si
     * {@code app.tenant.demo-switch-enabled} está en true y el request trae
     * este header con un slug válido, se usa ESE tenant en vez del único
     * activo. No es resolución real por dominio — es un atajo para mostrar
     * varias tiendas locales sin subdominios/DNS.
     */
    public static final String DEMO_TENANT_HEADER = "X-Demo-Tenant";

    private final TenantService tenantService;
    private final AppProperties props;

    public TenantResolutionFilter(TenantService tenantService, AppProperties props) {
        this.tenantService = tenantService;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            TenantContext.set(resolveTenantId(request));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantId(HttpServletRequest request) {
        if (props.getTenant().isDemoSwitchEnabled()) {
            String slug = request.getHeader(DEMO_TENANT_HEADER);
            if (slug != null && !slug.isBlank()) {
                String id = tenantService.resolveIdBySlug(slug.trim());
                if (id != null) return id;
            }
        }
        return tenantService.resolveCurrentTenantId();
    }
}
