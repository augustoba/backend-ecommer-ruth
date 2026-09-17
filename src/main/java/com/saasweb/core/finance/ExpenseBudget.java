package com.saasweb.core.finance;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Presupuesto mensual fijo por categoría de gasto — una fila por categoría
 * (no por mes), se compara contra el gasto del mes en curso (ítem 21).
 */
@Entity
@Table(name = "expense_budget", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "category_option_id"}))
public class ExpenseBudget {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "category_option_id", nullable = false)
    private String categoryOptionId;

    @Column(name = "monthly_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAmount;

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

    public String getCategoryOptionId() {
        return categoryOptionId;
    }

    public void setCategoryOptionId(String categoryOptionId) {
        this.categoryOptionId = categoryOptionId;
    }

    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
    }

    public void setMonthlyAmount(BigDecimal monthlyAmount) {
        this.monthlyAmount = monthlyAmount;
    }
}
