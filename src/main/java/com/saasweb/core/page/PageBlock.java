package com.saasweb.core.page;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Un bloque de una página del sitio (hoy: sólo la home, `pageType="HOME"`).
 * Ver PLAN_SAAS.md Fase 7. El dueño/a puede mostrar/ocultar bloques y
 * cambiarles el orden sin tocar código.
 *
 * <p>Primer bloque implementado: <b>HERO</b> — envuelve el carrusel que ya
 * existe ({@code core.hero.HeroSlide}), sin duplicar esas imágenes acá.
 * No tiene `configuration` todavía: se agrega el día que un bloque
 * realmente necesite datos propios (ej. un bloque de texto libre) — no
 * antes, para no construir sobre un formato que todavía no se sabe si
 * hace falta.</p>
 */
@Entity
@Table(name = "page_block")
public class PageBlock {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Página a la que pertenece. Hoy sólo existe "HOME". */
    @Column(nullable = false, length = 40)
    private String pageType;

    /** Tipo de bloque. Hoy sólo existe "HERO". */
    @Column(nullable = false, length = 40)
    private String blockType;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
