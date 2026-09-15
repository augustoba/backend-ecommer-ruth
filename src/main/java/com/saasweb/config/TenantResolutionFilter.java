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

    private final TenantService tenantService;

    public TenantResolutionFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            TenantContext.set(tenantService.resolveCurrentTenantId());
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
