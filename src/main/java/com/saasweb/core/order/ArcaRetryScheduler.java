package com.saasweb.core.order;

import com.saasweb.common.TenantContext;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.core.tenant.Tenant;
import com.saasweb.core.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta facturar con ARCA las ventas presenciales que quedaron en ticket
 * interno por un error de ARCA (rechazo, falla de conexión) — mismo patrón
 * tenant-loop que {@code MarketingCampaignScheduler}. No toca pedidos que
 * nunca tuvieron ARCA configurado a propósito, ni cobra ni descuenta stock de
 * nuevo (ver {@code OrderService.retryInvoicing}, ya usado por el botón
 * manual del panel — ítem 5).
 */
@Component
public class ArcaRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ArcaRetryScheduler.class);

    private final TenantRepository tenants;
    private final SiteSettingsService siteSettingsService;
    private final OrderRepository orderRepo;
    private final OrderService orderService;

    public ArcaRetryScheduler(TenantRepository tenants, SiteSettingsService siteSettingsService,
                              OrderRepository orderRepo, OrderService orderService) {
        this.tenants = tenants;
        this.siteSettingsService = siteSettingsService;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
    }

    /** Cada 6 horas. Cron fijo (cambiarlo requiere redeploy). */
    @Scheduled(cron = "0 0 */6 * * *")
    public void runPeriodically() {
        for (Tenant t : tenants.findAll()) {
            if (!t.isActive()) continue;
            TenantContext.set(t.getId());
            try {
                runFor(t);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void runFor(Tenant t) {
        if (!"FACTURA_ARCA".equals(siteSettingsService.get().getInvoiceMode())) return;
        for (Order o : orderRepo.findByTenantIdAndChannelAndStatusAndInvoiceTypeAndInvoiceErrorIsNotNull(
                t.getId(), SaleChannel.LOCAL, OrderStatus.PROCESADO, "TICKET_INTERNO")) {
            try {
                orderService.retryInvoicing(o.getId());
                log.info("ARCA: reintento automático OK para el pedido '{}' (tenant '{}').", o.getCode(), t.getSlug());
            } catch (Exception e) {
                log.debug("ARCA: reintento automático sigue fallando para '{}': {}", o.getCode(), e.getMessage());
            }
        }
    }
}
