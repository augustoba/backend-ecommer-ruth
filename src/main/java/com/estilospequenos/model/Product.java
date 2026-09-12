package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product")
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String ageRange;

    /**
     * Fotos del producto, en orden. La primera es la portada (la que se ve en
     * las tarjetas del catálogo, el carrito y el listado del panel). Cada valor
     * puede ser una URL o un data URI (imagen embebida) — por eso el largo.
     */
    @ElementCollection
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "idx")
    @Column(name = "url", nullable = false, length = 5_000_000)
    private List<String> images = new ArrayList<>();

    /** Link a un video de la prenda (YouTube). Opcional — se muestra embebido en la ficha. */
    @Column(length = 500)
    private String videoUrl;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * true = el dueño/a decidió no reponer más este producto. Se sigue vendiendo
     * mientras tenga stock (no cambia `active`), pero deja de aparecer en las
     * alertas de "por reponer". Se cambia desde la lista de reposición del panel
     * o editando el producto.
     */
    @Column(nullable = false)
    private boolean discontinued = false;

    /**
     * true = producto archivado (soft-delete). Sale del catálogo y de todos los
     * listados del panel, pero se conserva la fila para no romper el historial
     * de pedidos. Se puede restaurar desde "Productos archivados".
     */
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Escala de talle elegida (id de SizeScale). */
    private String sizeScaleId;

    /** Proveedor (id de Supplier) — info interna del admin. */
    private String supplierId;

    /** Precio de compra al proveedor — info interna del admin. */
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    /**
     * A partir de cuántas unidades por talle este producto se considera "stock
     * bajo" (para las alertas de reposición). null = usar el default global (3).
     */
    private Integer lowStockThreshold;

    @ElementCollection
    @CollectionTable(name = "product_size_stock", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "idx")
    private List<SizeStock> sizeStocks = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_param", joinColumns = @JoinColumn(name = "product_id"))
    private Set<ProductParam> params = new LinkedHashSet<>();

    /** Portada: la primera foto, o null si todavía no tiene ninguna. */
    @Transient
    public String getImageUrl() {
        return images.isEmpty() ? null : images.get(0);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public void setDiscontinued(boolean discontinued) {
        this.discontinued = discontinued;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getSizeScaleId() {
        return sizeScaleId;
    }

    public void setSizeScaleId(String sizeScaleId) {
        this.sizeScaleId = sizeScaleId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public List<SizeStock> getSizeStocks() {
        return sizeStocks;
    }

    public void setSizeStocks(List<SizeStock> sizeStocks) {
        this.sizeStocks = sizeStocks;
    }

    public Set<ProductParam> getParams() {
        return params;
    }

    public void setParams(Set<ProductParam> params) {
        this.params = params;
    }
}
