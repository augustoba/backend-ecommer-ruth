package com.saasweb.core.arca;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Nota de crédito emitida contra un pedido ya facturado con ARCA — aparte de
 * {@code Order} (no embebida) porque puede haber más de una NC parcial sobre
 * el mismo pedido. Acción manual desde el detalle de pedido (ver
 * {@code CreditNoteService.emit}), no automática: la relación entre un
 * cambio de prenda y si corresponde NC es ambigua (a veces la diferencia se
 * cobra, a veces no), mejor que lo decida el dueño/a explícitamente.
 */
@Entity
@Table(name = "credit_note")
public class CreditNote {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String reason;

    /** "NC_A" | "NC_B" | "NC_C" — el tipo que se INTENTÓ emitir, se haya aprobado o no. */
    @Column(length = 20, nullable = false)
    private String type;

    private String cae;

    @Column(name = "cae_vencimiento", length = 10)
    private String caeVencimiento;

    private Long number;

    @Column(name = "punto_venta")
    private Integer puntoVenta;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(length = 500)
    private String error;

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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public String getCaeVencimiento() {
        return caeVencimiento;
    }

    public void setCaeVencimiento(String caeVencimiento) {
        this.caeVencimiento = caeVencimiento;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Integer getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(Integer puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
