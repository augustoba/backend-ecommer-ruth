package com.estilospequenos.dto;

import com.estilospequenos.model.Expense;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ExpenseDtos {

    private ExpenseDtos() {}

    public record ExpenseRequest(
            @NotNull LocalDate date,
            String categoryOptionId,
            @NotNull @Positive BigDecimal amount,
            String description,
            Boolean repeatMonthly
    ) {}

    public record ExpenseResponse(
            String id, LocalDate date, String categoryOptionId, BigDecimal amount,
            String description, boolean repeatMonthly, String recurringGroupId
    ) {
        public static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getDate(), e.getCategoryOptionId(), e.getAmount(),
                    e.getDescription(), e.isRepeatMonthly(), e.getRecurringGroupId());
        }
    }
}
