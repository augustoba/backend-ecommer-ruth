package com.saasweb.core.tenant;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Un comercio/tienda de la plataforma. Hoy (ver PLAN_SAAS.md) existe una
 * única fila — la app entera resuelve siempre contra ese tenant (ver
 * {@code TenantResolutionFilter}), sembrada por {@code TenantService} a
 * partir de {@code app.tenant.*}. El día que haya resolución por dominio,
 * esta entidad suma `domain`/`subdomain`/`theme`; por ahora tiene sólo lo
 * mínimo para no construir sobre requisitos que todavía no existen.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false)
    private String name;

    /** Plan asignado (ver {@code core.plan.Plan}). Nunca null tras el seed. */
    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
