package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Descuento configurable. Dos tipos:
 *  - MONTO: por monto de compra ("compra mayor a $X → Y% off"), a nivel carrito.
 *  - PARAMETRO: por parametría ("todo lo de bebé 15% off"), por ítem del carrito.
 *
 * Vigencia opcional (`startsAt` / `endsAt`, inclusivas): si están, el descuento
 * sólo aplica en ese rango, además de estar `enabled`.
 */
@Entity
@Table(name = "discount")
@Getter
@Setter
@NoArgsConstructor
public class Discount {

    public enum Kind { MONTO, PARAMETRO }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(nullable = false)
    private int discountPercent;

    @Column(nullable = false)
    private boolean enabled = true;

    private String label;

    /** Desde cuándo aplica (inclusive). null = sin límite. */
    private LocalDate startsAt;

    /** Hasta cuándo aplica (inclusive). null = sin límite. */
    private LocalDate endsAt;

    /** kind == MONTO */
    private BigDecimal minAmount;

    /** kind == PARAMETRO */
    private String groupId;
    private String optionId;

    /** true si hoy el descuento está vigente (habilitado + dentro del rango de fechas). */
    public boolean activeNow() {
        return activeOn(LocalDate.now());
    }

    public boolean activeOn(LocalDate day) {
        return enabled
                && (startsAt == null || !day.isBefore(startsAt))
                && (endsAt == null || !day.isAfter(endsAt));
    }
}
