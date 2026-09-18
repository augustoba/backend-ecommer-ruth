package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Presupuesto mensual fijo por categoría de gasto — una fila por categoría
 * (no por mes), se compara contra el gasto del mes en curso.
 */
@Entity
@Table(name = "expense_budget", uniqueConstraints = @UniqueConstraint(columnNames = {"category_option_id"}))
public class ExpenseBudget {

    @Id
    private String id;

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
