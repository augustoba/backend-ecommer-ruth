package com.saasweb.core.finance;

import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Genera la instancia mensual de los gastos "repetir cada mes", para cada
 * tenant activo — mismo patrón que {@code MarketingCampaignScheduler}: un job
 * @Scheduled no pasa por {@code TenantResolutionFilter}, hay que resolver el
 * tenant a mano.
 */
@Component
public class ExpenseRecurrenceScheduler {

    private final ExpenseService expenseService;
    private final TenantRepository tenants;

    public ExpenseRecurrenceScheduler(ExpenseService expenseService, TenantRepository tenants) {
        this.expenseService = expenseService;
        this.tenants = tenants;
    }

    /** 1° de cada mes, 04:00 hora del servidor. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 4 1 * *")
    public void runMonthly() {
        for (Tenant t : tenants.findAll()) {
            if (!t.isActive()) continue;
            TenantContext.set(t.getId());
            try {
                expenseService.generateRecurringForCurrentMonth();
            } finally {
                TenantContext.clear();
            }
        }
    }
}
