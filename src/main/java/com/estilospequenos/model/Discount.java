package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Descuento configurable. Dos tipos:
 *  - MONTO: por monto de compra ("compra mayor a $X → Y% off"), a nivel carrito.
 *  - PARAMETRO: por parametría ("todo lo de bebé 15% off"), por ítem del carrito.
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

    /** kind == MONTO */
    private BigDecimal minAmount;

    /** kind == PARAMETRO */
    private String groupId;
    private String optionId;
}
