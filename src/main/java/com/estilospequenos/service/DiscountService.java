package com.estilospequenos.service;

import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.DiscountDtos.BreakdownLine;
import com.estilospequenos.dto.DiscountDtos.CartDiscountResult;
import com.estilospequenos.dto.DiscountDtos.ConfigRequest;
import com.estilospequenos.dto.DiscountDtos.DiscountRequest;
import com.estilospequenos.model.Discount;
import com.estilospequenos.model.DiscountConfig;
import com.estilospequenos.repository.DiscountConfigRepository;
import com.estilospequenos.repository.DiscountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Descuentos por monto y por parametría. Es el port de
 * `frontend/src/app/core/services/discount.service.ts` (`computeCartDiscount`).
 */
@Service
@Transactional
public class DiscountService {

    private final DiscountRepository repo;
    private final DiscountConfigRepository configRepo;

    public DiscountService(DiscountRepository repo, DiscountConfigRepository configRepo) {
        this.repo = repo;
        this.configRepo = configRepo;
    }

    // --- CRUD ---

    @Transactional(readOnly = true)
    public List<Discount> findAll() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Discount get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Descuento", id));
    }

    public Discount create(DiscountRequest req) {
        Discount d = new Discount();
        d.setId(UUID.randomUUID().toString());
        apply(d, req);
        return repo.save(d);
    }

    public Discount update(String id, DiscountRequest req) {
        Discount d = get(id);
        apply(d, req);
        return repo.save(d);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    private void apply(Discount d, DiscountRequest req) {
        d.setKind(req.kind());
        d.setDiscountPercent(req.discountPercent());
        d.setEnabled(req.enabled() == null || req.enabled());
        d.setLabel(req.label() == null || req.label().isBlank() ? null : req.label().trim());
        d.setMinAmount(req.kind() == Discount.Kind.MONTO ? req.minAmount() : null);
        d.setGroupId(req.kind() == Discount.Kind.PARAMETRO ? req.groupId() : null);
        d.setOptionId(req.kind() == Discount.Kind.PARAMETRO ? req.optionId() : null);
    }

    @Transactional(readOnly = true)
    public DiscountConfig getConfig() {
        return configRepo.findById(DiscountConfig.SINGLETON_ID)
                .orElseGet(() -> configRepo.save(new DiscountConfig()));
    }

    public DiscountConfig setConfig(ConfigRequest req) {
        DiscountConfig c = getConfig();
        c.setCombineMode(req.combineMode());
        return configRepo.save(c);
    }

    // --- Cálculo ---

    /** Un ítem del carrito para el cálculo de descuentos. */
    public record CartLineInput(BigDecimal unitPrice, int quantity, Map<String, List<String>> params) {}

    @Transactional(readOnly = true)
    public CartDiscountResult computeForLines(List<CartLineInput> items) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.signum() <= 0) {
            return new CartDiscountResult(0, BigDecimal.ZERO, List.of());
        }

        List<Discount> paramDiscounts = repo.findByKindAndEnabledTrue(Discount.Kind.PARAMETRO).stream()
                .filter(d -> d.getGroupId() != null && d.getOptionId() != null)
                .toList();

        // 1) Descuento por parámetro, por ítem (el % más alto que aplica).
        Map<String, BigDecimal> paramByDiscountId = new LinkedHashMap<>();
        BigDecimal paramTotal = BigDecimal.ZERO;
        for (CartLineInput item : items) {
            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            int bestPct = 0;
            String bestId = null;
            for (Discount d : paramDiscounts) {
                boolean applies = item.params() != null
                        && item.params().getOrDefault(d.getGroupId(), List.of()).contains(d.getOptionId());
                if (applies && d.getDiscountPercent() > bestPct) {
                    bestPct = d.getDiscountPercent();
                    bestId = d.getId();
                }
            }
            if (bestId != null && bestPct > 0) {
                BigDecimal amount = pctOf(lineTotal, bestPct);
                paramTotal = paramTotal.add(amount);
                paramByDiscountId.merge(bestId, amount, BigDecimal::add);
            }
        }

        DiscountConfig.CombineMode mode = getConfig().getCombineMode();

        // 2) Descuento por monto.
        BigDecimal amountBase = mode == DiscountConfig.CombineMode.COMBINAR
                ? subtotal.subtract(paramTotal) : subtotal;
        Discount amountTier = bestAmountTierFor(amountBase);
        BigDecimal amountValue = amountTier != null
                ? pctOf(amountBase, amountTier.getDiscountPercent()) : BigDecimal.ZERO;

        // 3) Combinar.
        List<BreakdownLine> breakdown = new ArrayList<>();
        BigDecimal discountAmount;

        if (mode == DiscountConfig.CombineMode.COMBINAR) {
            addParamLines(breakdown, paramByDiscountId);
            addAmountLine(breakdown, amountTier, amountValue);
            discountAmount = paramTotal.add(amountValue);
        } else if (paramTotal.compareTo(amountValue) >= 0) {
            addParamLines(breakdown, paramByDiscountId);
            discountAmount = paramTotal;
        } else {
            addAmountLine(breakdown, amountTier, amountValue);
            discountAmount = amountValue;
        }

        discountAmount = discountAmount.min(subtotal);
        int discountPercent = discountAmount.signum() > 0
                ? discountAmount.multiply(BigDecimal.valueOf(100))
                    .divide(subtotal, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        return new CartDiscountResult(discountPercent, discountAmount, breakdown);
    }

    private Discount bestAmountTierFor(BigDecimal base) {
        return repo.findByKindAndEnabledTrue(Discount.Kind.MONTO).stream()
                .filter(d -> d.getMinAmount() != null && d.getMinAmount().signum() > 0)
                .filter(d -> base.compareTo(d.getMinAmount()) >= 0)
                .max(Comparator.comparing(Discount::getMinAmount))
                .orElse(null);
    }

    /** El próximo tier por monto todavía no alcanzado (para el banner del carrito). */
    @Transactional(readOnly = true)
    public Discount nextAmountTierFor(BigDecimal subtotal) {
        return repo.findByKindAndEnabledTrue(Discount.Kind.MONTO).stream()
                .filter(d -> d.getMinAmount() != null && d.getMinAmount().signum() > 0)
                .filter(d -> subtotal.compareTo(d.getMinAmount()) < 0)
                .min(Comparator.comparing(Discount::getMinAmount))
                .orElse(null);
    }

    private void addParamLines(List<BreakdownLine> breakdown, Map<String, BigDecimal> byId) {
        byId.forEach((id, amount) -> {
            if (amount.signum() > 0) {
                Discount d = repo.findById(id).orElse(null);
                breakdown.add(new BreakdownLine(paramLabel(d), amount));
            }
        });
    }

    private void addAmountLine(List<BreakdownLine> breakdown, Discount tier, BigDecimal value) {
        if (tier != null && value.signum() > 0) {
            breakdown.add(new BreakdownLine(
                    "Compra mayor a $" + tier.getMinAmount().toBigInteger()
                            + " (" + tier.getDiscountPercent() + "%)",
                    value));
        }
    }

    private static String paramLabel(Discount d) {
        if (d == null) return "Descuento";
        if (d.getLabel() != null) return d.getLabel();
        return "Descuento por parametría (" + d.getDiscountPercent() + "%)";
    }

    private static BigDecimal pctOf(BigDecimal base, int percent) {
        return base.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
}
