package com.estilospequenos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Corre la campaña de marketing una vez por día. Ver {@link MarketingCampaignService#runNow()}. */
@Component
public class MarketingCampaignScheduler {

    private final MarketingCampaignService campaignService;

    public MarketingCampaignScheduler(MarketingCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /** 06:00 hora del servidor, antes de que abra el local. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 6 * * *")
    public void runDaily() {
        campaignService.runNow();
    }
}
