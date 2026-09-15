package com.saasweb.core.marketing;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.marketing.MarketingDtos.MarketingConfigRequest;
import com.saasweb.core.marketing.MarketingConfig;
import com.saasweb.core.marketing.MarketingConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MarketingConfigService {

    private final MarketingConfigRepository repo;

    public MarketingConfigService(MarketingConfigRepository repo) {
        this.repo = repo;
    }

    /** Devuelve la config del tenant actual; si no existe, la crea con los valores por defecto. */
    public MarketingConfig get() {
        String tenantId = TenantContext.getTenantId();
        return repo.findById(tenantId).orElseGet(() -> {
            MarketingConfig c = new MarketingConfig();
            c.setId(tenantId);
            return repo.save(c);
        });
    }

    public MarketingConfig update(MarketingConfigRequest req) {
        if (req.spendThreshold().signum() < 0) {
            throw new BadRequestException("El monto para VIP no puede ser negativo.");
        }
        MarketingConfig c = get();
        c.setEnabled(Boolean.TRUE.equals(req.enabled()));
        c.setDiscountPercent(req.discountPercent());
        c.setInactivityDays(req.inactivityDays());
        c.setSpendThreshold(req.spendThreshold());
        c.setDailyEmailCap(req.dailyEmailCap());
        c.setCouponValidityDays(req.couponValidityDays());
        c.setCooldownDays(req.cooldownDays());
        c.setEmailSubject(blankToNull(req.emailSubject()));
        c.setEmailBody(blankToNull(req.emailBody()));
        c.setEmailImageUrl(blankToNull(req.emailImageUrl()));
        return repo.save(c);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
