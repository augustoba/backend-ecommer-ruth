package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Registro de un envío (o intento de envío) de campaña de marketing a un email. */
@Entity
@Table(name = "marketing_send")
@Getter
@Setter
@NoArgsConstructor
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
}
