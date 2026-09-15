package com.saasweb.core.marketing;

import com.saasweb.common.TenantContext;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre la campaña de marketing una vez por día, para cada tenant activo. Ver
 * {@link MarketingCampaignService#runNow()}. A diferencia de un request HTTP,
 * un job @Scheduled no pasa por {@code TenantResolutionFilter} — por eso acá
 * hay que resolver el tenant a mano (hoy: uno solo; el loop ya deja listo el
 * caso de varios).
 */
@Component
public class MarketingCampaignScheduler {

    private final MarketingCampaignService campaignService;
    private final TenantRepository tenants;

    public MarketingCampaignScheduler(MarketingCampaignService campaignService, TenantRepository tenants) {
        this.campaignService = campaignService;
        this.tenants = tenants;
    }

    /** 06:00 hora del servidor, antes de que abra el local. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 6 * * *")
    public void runDaily() {
        for (Tenant t : tenants.findAll()) {
            if (!t.isActive()) continue;
            TenantContext.set(t.getId());
            try {
                campaignService.runNow();
            } finally {
                TenantContext.clear();
            }
        }
    }
}
