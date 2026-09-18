package com.estilospequenos.service;

import com.estilospequenos.dto.DashboardDtos.LowStockItem;
import com.estilospequenos.model.SiteSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manda un mail diario con los talles en stock bajo, si la tienda lo activó.
 * No manda nada si no cargó {@code lowStockAlertEmail} o no activó
 * {@code lowStockAlertEnabled}.
 */
@Component
public class LowStockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertScheduler.class);

    private final SiteSettingsService siteSettingsService;
    private final DashboardService dashboardService;
    private final LowStockAlertMailService mailService;

    public LowStockAlertScheduler(SiteSettingsService siteSettingsService,
                                  DashboardService dashboardService,
                                  LowStockAlertMailService mailService) {
        this.siteSettingsService = siteSettingsService;
        this.dashboardService = dashboardService;
        this.mailService = mailService;
    }

    /** 07:00 hora del servidor, antes de que abra el local. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 7 * * *")
    public void runDaily() {
        SiteSettings s = siteSettingsService.get();
        if (!s.isLowStockAlertEnabled()) return;
        String email = s.getLowStockAlertEmail();
        if (email == null || email.isBlank()) return;

        List<LowStockItem> items = dashboardService.lowStock();
        if (items.isEmpty()) return;

        mailService.send(email, items);
        log.info("Alerta de stock bajo mandada a '{}' ({} talles).", email, items.size());
    }
}
