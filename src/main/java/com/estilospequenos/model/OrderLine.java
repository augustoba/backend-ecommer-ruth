package com.estilospequenos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    /** Nombre del producto al momento del pedido (por si luego se edita/borra). */
    @Column(nullable = false)
    private String productName;

    @Column(name = "size_value", nullable = false)
    private String size;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Costo del producto congelado al CONFIRMAR el pedido (no al crearlo) —
     * para que la ganancia histórica no cambie si después se actualiza
     * {@code Product.costPrice}. {@code null} = el producto no tenía costo
     * cargado en ese momento (no se puede saber el margen de esta línea).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** El dueño/a lo tilda para confirmar que hay stock y lo va a entregar. */
    @Column(nullable = false)
    private boolean accepted = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
