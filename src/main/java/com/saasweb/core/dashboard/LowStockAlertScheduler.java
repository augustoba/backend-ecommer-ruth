package com.saasweb.core.dashboard;

import com.saasweb.common.TenantContext;
import com.saasweb.core.product.LowStockAlertMailService;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manda un mail diario con los talles en stock bajo, para el tenant que lo
 * tenga habilitado — mismo patrón tenant-loop que
 * {@code MarketingCampaignScheduler}. No manda nada si la tienda no cargó
 * `lowStockAlertEmail` o no activó `lowStockAlertEnabled` (ítem 10).
 */
@Component
public class LowStockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertScheduler.class);

    private final TenantRepository tenants;
    private final SiteSettingsService siteSettingsService;
    private final DashboardService dashboardService;
    private final LowStockAlertMailService mailService;

    public LowStockAlertScheduler(TenantRepository tenants, SiteSettingsService siteSettingsService,
                                  DashboardService dashboardService, LowStockAlertMailService mailService) {
        this.tenants = tenants;
        this.siteSettingsService = siteSettingsService;
        this.dashboardService = dashboardService;
        this.mailService = mailService;
    }

    /** 07:00 hora del servidor, antes de que abra el local. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 7 * * *")
    public void runDaily() {
        for (Tenant t : tenants.findAll()) {
            if (!t.isActive()) continue;
            TenantContext.set(t.getId());
            try {
                runFor(t);
            } catch (Exception e) {
                log.warn("Alerta de stock bajo: fallo para tenant '{}': {}", t.getSlug(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void runFor(Tenant t) {
        SiteSettings s = siteSettingsService.get();
        if (!s.isLowStockAlertEnabled()) return;
        String email = s.getLowStockAlertEmail();
        if (email == null || email.isBlank()) return;

        List<DashboardDtos.LowStockItem> items = dashboardService.lowStock();
        if (items.isEmpty()) return;

        mailService.send(email, items);
        log.info("Alerta de stock bajo mandada a '{}' para tenant '{}' ({} talles).", email, t.getSlug(), items.size());
    }
}
