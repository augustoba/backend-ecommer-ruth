package com.saasweb.core.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
    Optional<Expense> findByIdAndTenantId(String id, String tenantId);

    List<Expense> findByTenantIdOrderByDateDesc(String tenantId);

    List<Expense> findByTenantIdAndDateBetweenOrderByDateDesc(String tenantId, LocalDate from, LocalDate to);

    /** Gastos con recurrencia activa — usado por {@link ExpenseRecurrenceScheduler}. */
    List<Expense> findByTenantIdAndRepeatMonthlyTrue(String tenantId);

    /** Ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);
}
