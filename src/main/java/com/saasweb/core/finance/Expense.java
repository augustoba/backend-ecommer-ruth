package com.saasweb.core.finance;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Gasto del emprendimiento (alquiler, sueldos, mercadería, etc.). La
 * categoría es una opción de la parametría genérica "Categoría de gasto"
 * (id {@code categoryOptionId}, FK "blanda" a {@code ParamOption} — mismo
 * patrón que {@code Product.supplierId}), editable por tienda desde
 * {@code /admin/param-groups} como cualquier otra parametría.
 */
@Entity
@Table(name = "expense")
public class Expense {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(name = "category_option_id")
    private String categoryOptionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 2000)
    private String description;

    /** true = un scheduler mensual genera sola la instancia del mes siguiente. */
    @Column(name = "repeat_monthly", nullable = false)
    private boolean repeatMonthly = false;

    /**
     * Mismo valor para todas las instancias de una serie recurrente (la
     * primera instancia se autoasigna su propio id). {@code null} en gastos
     * que nunca fueron recurrentes.
     */
    @Column(name = "recurring_group_id")
    private String recurringGroupId;

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCategoryOptionId() {
        return categoryOptionId;
    }

    public void setCategoryOptionId(String categoryOptionId) {
        this.categoryOptionId = categoryOptionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRepeatMonthly() {
        return repeatMonthly;
    }

    public void setRepeatMonthly(boolean repeatMonthly) {
        this.repeatMonthly = repeatMonthly;
    }

    public String getRecurringGroupId() {
        return recurringGroupId;
    }

    public void setRecurringGroupId(String recurringGroupId) {
        this.recurringGroupId = recurringGroupId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
