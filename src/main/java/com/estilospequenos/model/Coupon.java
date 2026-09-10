package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cupón de descuento: un código que el cliente escribe en el carrito (distinto
 * de los descuentos automáticos de {@link Discount}).
 *
 * <p>Puede ser un código de un solo uso (generado en lote para regalar) o un
 * código compartido con tope de usos (ej: NAVIDAD10, 100 usos). El descuento es
 * un porcentaje o un monto fijo. {@code stackable} decide si se combina con los
 * descuentos automáticos o si se aplica sólo el que más conviene.</p>
 */
@Entity
@Table(name = "coupon")
public class Coupon {

    public enum Kind { PERCENT, AMOUNT }

    @Id
    private String id;

    /** Código en mayúsculas, único. */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Kind kind = Kind.PERCENT;

    /** Porcentaje (0-100) si kind = PERCENT; monto en ARS si kind = AMOUNT. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value = BigDecimal.ZERO;

    /** Subtotal mínimo para poder usarlo. null = sin mínimo. */
    @Column(precision = 12, scale = 2)
    private BigDecimal minAmount;

    /** Cantidad máxima de usos. null = ilimitado. */
    private Integer maxUses;

    @Column(nullable = false)
    private int usedCount = 0;

    /** Último día en que se puede usar (inclusive). null = sin vencimiento. */
    private LocalDate expiresAt;

    @Column(nullable = false)
    private boolean enabled = true;

    /** true = se combina con los descuentos automáticos; false = se aplica sólo el que más ahorra. */
    @Column(nullable = false)
    private boolean stackable = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Texto opcional para mostrar en el carrito / recibo. */
    @Column(length = 200)
    private String label;

    public boolean isExhausted() {
        return maxUses != null && usedCount >= maxUses;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDate.now().isAfter(expiresAt);
    }

    /** Usable ahora: habilitado, no vencido y con usos disponibles. */
    public boolean isUsable() {
        return enabled && !isExpired() && !isExhausted();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
