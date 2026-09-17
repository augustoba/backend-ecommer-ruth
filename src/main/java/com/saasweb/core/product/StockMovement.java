package com.saasweb.core.product;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una fila por cada cambio de stock de un talle — auditoría de qué pasó,
 * no sólo el número actual. Se registra desde un único punto
 * ({@link ProductService#decrementStock}/{@code incrementStock}/{@code setStock})
 * para que ningún caller se pueda olvidar.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    /** Nombre del producto al momento del movimiento (por si se edita/borra después). */
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "size_value", nullable = false)
    private String size;

    /** Positivo = entró stock, negativo = salió. */
    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockMovementReason reason;

    /** Sólo para AJUSTE_MANUAL: motivo escrito por el admin. */
    @Column(length = 500)
    private String note;

    /** orderId/exchangeId cuando el movimiento viene de una venta/cambio. */
    @Column(name = "reference_id")
    private String referenceId;

    /** Sólo para ENTRADA_COMPRA: costo unitario de esa compra. */
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "created_by_dni")
    private String createdByDni;

    @Column(name = "created_by_name")
    private String createdByName;

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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(int quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public StockMovementReason getReason() {
        return reason;
    }

    public void setReason(StockMovementReason reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getCreatedByDni() {
        return createdByDni;
    }

    public void setCreatedByDni(String createdByDni) {
        this.createdByDni = createdByDni;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
