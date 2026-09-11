package com.estilospequenos.service;

import com.estilospequenos.dto.CouponDtos.CouponRequest;
import com.estilospequenos.dto.MarketingDtos.CandidatePreview;
import com.estilospequenos.dto.MarketingDtos.PreviewResult;
import com.estilospequenos.dto.MarketingDtos.RunResult;
import com.estilospequenos.model.Coupon;
import com.estilospequenos.model.MarketingConfig;
import com.estilospequenos.model.MarketingSend;
import com.estilospequenos.repository.MarketingSendRepository;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.OrderRepository.CustomerAggregateRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lógica central de las campañas automáticas: calcula quién califica (inactivos
 * / VIP), aplica cooldown y tope diario, genera el cupón personalizado y manda
 * el mail. {@link #previewToday()} es de sólo lectura (para mostrar en el admin
 * antes de mandar nada); {@link #runNow()} ejecuta de verdad y la usan tanto el
 * job diario como el botón "mandar ahora" del panel.
 */
@Service
public class MarketingCampaignService {

    private static final Logger log = LoggerFactory.getLogger(MarketingCampaignService.class);

    private final OrderRepository orderRepo;
    private final MarketingConfigService configService;
    private final MarketingSendRepository sendRepo;
    private final CouponService couponService;
    private final MarketingMailService mailService;

    public MarketingCampaignService(OrderRepository orderRepo, MarketingConfigService configService,
                                     MarketingSendRepository sendRepo, CouponService couponService,
                                     MarketingMailService mailService) {
        this.orderRepo = orderRepo;
        this.configService = configService;
        this.sendRepo = sendRepo;
        this.couponService = couponService;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public PreviewResult previewToday() {
        MarketingConfig cfg = configService.get();
        if (!cfg.isEnabled()) {
            return new PreviewResult(0, 0, 0, 0, 0, List.of());
        }
        Candidates computed = computeCandidates(cfg);
        long alreadySentToday = sentToday();
        int remainingCap = (int) Math.max(0L, cfg.getDailyEmailCap() - alreadySentToday);
        int willBeEmailed = Math.min(computed.list().size(), remainingCap);
        List<CandidatePreview> sample = computed.list().stream().limit(50).toList();
        return new PreviewResult(computed.list().size(), (int) alreadySentToday, remainingCap,
                computed.excludedByCooldown(), willBeEmailed, sample);
    }

    /** Corre de verdad: genera cupones y manda mails, respetando el tope diario. Sin reintentos. */
    @Transactional
    public RunResult runNow() {
        MarketingConfig cfg = configService.get();
        if (!cfg.isEnabled()) {
            return new RunResult(0, 0, 0, 0);
        }
        Candidates computed = computeCandidates(cfg);
        long alreadySentToday = sentToday();
        int remainingCap = (int) Math.max(0L, cfg.getDailyEmailCap() - alreadySentToday);
        List<CandidatePreview> toSend = computed.list().stream().limit(remainingCap).toList();
        int skippedCap = computed.list().size() - toSend.size();

        int sent = 0;
        int failed = 0;
        for (CandidatePreview candidate : toSend) {
            MarketingSend record = new MarketingSend();
            record.setId(UUID.randomUUID().toString());
            record.setEmail(candidate.email());
            record.setReason(candidate.reason());
            record.setLifetimeSpendSnapshot(candidate.lifetimeSpend());
            record.setLastOrderAtSnapshot(candidate.lastOrderAt());
            try {
                LocalDate expiresAt = LocalDate.now().plusDays(cfg.getCouponValidityDays());
                CouponRequest couponReq = new CouponRequest(
                        null, 1, null, Coupon.Kind.PERCENT, BigDecimal.valueOf(cfg.getDiscountPercent()),
                        null, 1, expiresAt, true, false,
                        candidate.reason() == MarketingSend.Reason.VIP ? "Campaña: VIP" : "Campaña: inactivo");
                Coupon coupon = couponService.create(couponReq).get(0);
                record.setCouponCode(coupon.getCode());
                mailService.sendCoupon(candidate.email(), cfg, coupon.getCode(), expiresAt);
                record.setStatus(MarketingSend.Status.SENT);
                sent++;
            } catch (Exception e) {
                record.setStatus(MarketingSend.Status.FAILED);
                record.setErrorMessage(truncate(e.getMessage()));
                failed++;
                log.warn("Fallo al mandar campaña de marketing a {}: {}", candidate.email(), e.getMessage());
            }
            sendRepo.save(record);
        }
        return new RunResult(toSend.size(), sent, failed, skippedCap);
    }

    private record Candidates(List<CandidatePreview> list, int excludedByCooldown) {}

    /**
     * Candidatos deduplicados (VIP tiene prioridad sobre inactivo si califica en
     * los dos), filtrados por cooldown, ordenados: VIP por gasto desc, luego
     * inactivos por antigüedad de última compra.
     */
    private Candidates computeCandidates(MarketingConfig cfg) {
        Instant cutoff = Instant.now().minus(cfg.getInactivityDays(), ChronoUnit.DAYS);
        List<CustomerAggregateRow> inactive = orderRepo.findInactiveCustomers(cutoff);
        List<CustomerAggregateRow> vip = orderRepo.findHighSpendCustomers(cfg.getSpendThreshold());

        LinkedHashMap<String, CandidatePreview> byEmail = new LinkedHashMap<>();
        for (CustomerAggregateRow row : vip) {
            byEmail.put(row.getEmail(), new CandidatePreview(row.getEmail(), MarketingSend.Reason.VIP,
                    row.getLifetimeSpend(), row.getLastOrderAt()));
        }
        for (CustomerAggregateRow row : inactive) {
            byEmail.putIfAbsent(row.getEmail(), new CandidatePreview(row.getEmail(), MarketingSend.Reason.INACTIVE,
                    row.getLifetimeSpend(), row.getLastOrderAt()));
        }

        Instant cooldownCutoff = Instant.now().minus(cfg.getCooldownDays(), ChronoUnit.DAYS);
        Set<String> recentlySent = new HashSet<>(sendRepo.emailsSentSince(cooldownCutoff));

        List<CandidatePreview> vipList = new ArrayList<>();
        List<CandidatePreview> inactiveList = new ArrayList<>();
        int excluded = 0;
        for (CandidatePreview c : byEmail.values()) {
            if (recentlySent.contains(c.email())) {
                excluded++;
                continue;
            }
            if (c.reason() == MarketingSend.Reason.VIP) vipList.add(c);
            else inactiveList.add(c);
        }
        vipList.sort(Comparator.comparing(CandidatePreview::lifetimeSpend).reversed());
        inactiveList.sort(Comparator.comparing(CandidatePreview::lastOrderAt));

        List<CandidatePreview> ordered = new ArrayList<>(vipList);
        ordered.addAll(inactiveList);
        return new Candidates(ordered, excluded);
    }

    private long sentToday() {
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
        return sendRepo.countBySentAtGreaterThanEqualAndSentAtLessThanAndStatus(
                startOfToday, startOfTomorrow, MarketingSend.Status.SENT);
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
