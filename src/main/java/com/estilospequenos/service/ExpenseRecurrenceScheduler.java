package com.estilospequenos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Genera la instancia mensual de los gastos "repetir cada mes". */
@Component
public class ExpenseRecurrenceScheduler {

    private final ExpenseService expenseService;

    public ExpenseRecurrenceScheduler(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** 1° de cada mes, 04:00 hora del servidor. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 4 1 * *")
    public void runMonthly() {
        expenseService.generateRecurringForCurrentMonth();
    }
}
