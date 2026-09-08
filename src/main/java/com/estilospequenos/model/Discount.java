package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Descuento configurable. Tipos:
 *  - MONTO: por monto de compra ("compra mayor a $X → Y% off"), a nivel carrito.
 *  - PARAMETRO: por parametría ("todo lo de bebé 15% off"), por ítem del carrito.
 *  - PAGO: por medio de pago elegido en el carrito (efectivo, transferencia…).
 *  - ENVIO_GRATIS: informativo — si el subtotal supera `minAmount`, se muestra
 *     "envío gratis" (+ el texto de `detail`). No descuenta plata: el envío no
 *     se cotiza en la web.
 *
 * `stackable`: si es acumulable con otros. Si en un carrito hay al menos un
 * descuento NO acumulable en juego, se aplica sólo el que más ahorra.
 *
 * Vigencia opcional (`startsAt` / `endsAt`, inclusivas).
 */
@Entity
@Table(name = "discount")
@Getter
@Setter
@NoArgsConstructor
public class Discount {

    public enum Kind { MONTO, PARAMETRO, PAGO, ENVIO_GRATIS }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(nullable = false)
    private int discountPercent;

    @Column(nullable = false)
    private boolean enabled = true;

    /** true si se puede combinar (sumar) con otros descuentos. */
    @Column(nullable = false)
    private boolean stackable = false;

    /** Nombre corto para el panel y el resumen del carrito. */
    private String label;

    /** Letra chica configurable (ej: "solo microcentro"). Se muestra al cliente. */
    @Column(length = 300)
    private String detail;

    /** Desde/hasta (inclusive). null = sin límite. */
    private LocalDate startsAt;
    private LocalDate endsAt;

    /** kind == MONTO o ENVIO_GRATIS: monto mínimo de subtotal. */
    private BigDecimal minAmount;

    /** kind == PARAMETRO */
    private String groupId;
    private String optionId;

    /** kind == PAGO: medios de pago a los que aplica, separados por coma (ej: "TRANSFER,CASH"). */
    @Column(length = 100)
    private String paymentMethods;

    public Set<PaymentMethod> paymentMethodSet() {
        if (paymentMethods == null || paymentMethods.isBlank()) return EnumSet.noneOf(PaymentMethod.class);
        Set<PaymentMethod> out = EnumSet.noneOf(PaymentMethod.class);
        for (String s : paymentMethods.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) {
                try { out.add(PaymentMethod.valueOf(v)); } catch (IllegalArgumentException ignored) { /* skip */ }
            }
        }
        return out;
    }

    public void setPaymentMethodSet(Set<PaymentMethod> methods) {
        this.paymentMethods = methods == null || methods.isEmpty()
                ? null
                : String.join(",", methods.stream().map(Enum::name).sorted().toList());
    }

    public boolean activeNow() {
        return activeOn(LocalDate.now());
    }

    public boolean activeOn(LocalDate day) {
        return enabled
                && (startsAt == null || !day.isBefore(startsAt))
                && (endsAt == null || !day.isAfter(endsAt));
    }

    /** Helper para el seeder / tests. */
    public static Set<PaymentMethod> methods(PaymentMethod... m) {
        return m.length == 0 ? EnumSet.noneOf(PaymentMethod.class) : EnumSet.copyOf(Arrays.asList(m));
    }
}
