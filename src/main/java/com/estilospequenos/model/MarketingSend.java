package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Registro de un envío (o intento de envío) de campaña de marketing a un email. */
@Entity
@Table(name = "marketing_send")
public class MarketingSend {

    public enum Reason { INACTIVE, VIP }

    public enum Status { SENT, FAILED }

    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Reason reason;

    /** null si falló antes de llegar a crear el cupón. */
    @Column(length = 40)
    private String couponCode;

    @Column(nullable = false)
    private Instant sentAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(length = 500)
    private String errorMessage;

    /** Snapshot al momento del envío, para el historial (no dependen de cambios posteriores). */
    @Column(precision = 12, scale = 2)
    private BigDecimal lifetimeSpendSnapshot;

    private Instant lastOrderAtSnapshot;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public BigDecimal getLifetimeSpendSnapshot() {
        return lifetimeSpendSnapshot;
    }

    public void setLifetimeSpendSnapshot(BigDecimal lifetimeSpendSnapshot) {
        this.lifetimeSpendSnapshot = lifetimeSpendSnapshot;
    }

    public Instant getLastOrderAtSnapshot() {
        return lastOrderAtSnapshot;
    }

    public void setLastOrderAtSnapshot(Instant lastOrderAtSnapshot) {
        this.lastOrderAtSnapshot = lastOrderAtSnapshot;
    }
}
