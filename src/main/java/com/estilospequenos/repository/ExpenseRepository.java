package com.estilospequenos.repository;

import com.estilospequenos.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findByOrderByDateDesc();

    List<Expense> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);

    /** Gastos con recurrencia activa — usado por {@code ExpenseRecurrenceScheduler}. */
    List<Expense> findByRepeatMonthlyTrue();
}
