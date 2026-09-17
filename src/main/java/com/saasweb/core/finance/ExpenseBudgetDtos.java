package com.saasweb.core.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class ExpenseBudgetDtos {

    private ExpenseBudgetDtos() {}

    public record BudgetRequest(@NotBlank String categoryOptionId, @NotNull @Positive BigDecimal monthlyAmount) {}

    public record BudgetResponse(String id, String categoryOptionId, BigDecimal monthlyAmount) {
        public static BudgetResponse from(ExpenseBudget b) {
            return new BudgetResponse(b.getId(), b.getCategoryOptionId(), b.getMonthlyAmount());
        }
    }

    /** Presupuesto vs. gastado en lo que va del mes actual — para el badge de alerta. */
    public record BudgetStatus(
            String budgetId, String categoryOptionId, BigDecimal monthlyAmount, BigDecimal spent, boolean exceeded) {}
}
