package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.dto.MarketingDtos.MarketingConfigRequest;
import com.estilospequenos.model.MarketingConfig;
import com.estilospequenos.repository.MarketingConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MarketingConfigService {

    private final MarketingConfigRepository repo;

    public MarketingConfigService(MarketingConfigRepository repo) {
        this.repo = repo;
    }

    /** Devuelve la config; si no existe, la crea con los valores por defecto. */
    public MarketingConfig get() {
        return repo.findById(MarketingConfig.SINGLETON_ID).orElseGet(() -> repo.save(new MarketingConfig()));
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
