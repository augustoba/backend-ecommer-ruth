package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.DiscountDtos.BreakdownLine;
import com.estilospequenos.dto.DiscountDtos.CartDiscountResult;
import com.estilospequenos.dto.DiscountDtos.DiscountRequest;
import com.estilospequenos.dto.DiscountDtos.FreeShipping;
import com.estilospequenos.model.DeliveryMethod;
import com.estilospequenos.model.Discount;
import com.estilospequenos.model.PaymentMethod;
import com.estilospequenos.repository.DiscountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Descuentos configurables. Es el port de
 * `frontend/src/app/core/services/discount.service.ts` (`computeCartDiscount`).
 *
 * Reglas de combinación:
 *  - Si TODOS los descuentos que aplican son acumulables → se combinan
 *    (compuestos: parámetro por ítem, luego monto, luego pago, cada uno sobre lo
 *    que va quedando — le da al vendedor el menor descuento posible).
 *  - Si hay al menos uno NO acumulable → se aplica sólo el que más ahorra.
 *  - ENVIO_GRATIS es aparte: informativo, no descuenta plata.
 */
@Service
@Transactional
public class DiscountService {

    private final DiscountRepository repo;

    public DiscountService(DiscountRepository repo) {
        this.repo = repo;
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
        if (req.startsAt() != null && req.endsAt() != null && req.startsAt().isAfter(req.endsAt())) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        Discount.Kind kind = req.kind();
        d.setKind(kind);
        d.setDiscountPercent(kind == Discount.Kind.ENVIO_GRATIS ? 0 : req.discountPercent());
        d.setEnabled(req.enabled() == null || req.enabled());
        d.setStackable(Boolean.TRUE.equals(req.stackable()));
        d.setLabel(blankToNull(req.label()));
        d.setDetail(blankToNull(req.detail()));
        d.setStartsAt(req.startsAt());
        d.setEndsAt(req.endsAt());
        d.setMinAmount(kind == Discount.Kind.MONTO || kind == Discount.Kind.ENVIO_GRATIS ? req.minAmount() : null);
        d.setGroupId(kind == Discount.Kind.PARAMETRO ? req.groupId() : null);
        d.setOptionId(kind == Discount.Kind.PARAMETRO ? req.optionId() : null);
        d.setPaymentMethodSet(kind == Discount.Kind.PAGO && req.paymentMethods() != null
                ? EnumSet.copyOf(req.paymentMethods().isEmpty()
                    ? EnumSet.noneOf(PaymentMethod.class) : req.paymentMethods())
                : null);
    }

    private List<Discount> activeOfKind(Discount.Kind kind) {
        return repo.findAll().stream()
                .filter(d -> d.getKind() == kind && d.activeNow())
                .toList();
    }

    // --- Cálculo ---

    /** Un ítem del carrito para el cálculo de descuentos. */
    public record CartLineInput(BigDecimal unitPrice, int quantity, Map<String, List<String>> params) {}

    /** Un descuento que aplica al carrito, con el monto que ahorraría si fuera solo (sobre el subtotal). */
    private record Instance(Discount discount, BigDecimal standaloneAmount, String breakdownLabel) {}

    @Transactional(readOnly = true)
    public CartDiscountResult computeForLines(List<CartLineInput> items,
                                              PaymentMethod paymentMethod, DeliveryMethod deliveryMethod) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FreeShipping freeShipping = freeShippingFor(subtotal, deliveryMethod);

        if (subtotal.signum() <= 0) {
            return new CartDiscountResult(0, BigDecimal.ZERO, List.of(), freeShipping);
        }

        // --- Descuento por parámetro: por ítem, el % más alto que aplica ---
        List<Discount> paramDiscounts = activeOfKind(Discount.Kind.PARAMETRO).stream()
                .filter(d -> d.getGroupId() != null && d.getOptionId() != null)
                .toList();
        Map<String, BigDecimal> paramByDiscountId = new LinkedHashMap<>();
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
                paramByDiscountId.merge(bestId, pctOf(lineTotal, bestPct), BigDecimal::add);
            }
        }

        // --- Instancias de descuento de plata (parámetro, monto, pago) ---
        List<Instance> instances = new ArrayList<>();
        paramByDiscountId.forEach((id, amount) -> {
            if (amount.signum() > 0) {
                Discount d = repo.findById(id).orElse(null);
                if (d != null) instances.add(new Instance(d, amount, paramLabel(d)));
            }
        });

        Discount amountTier = bestAmountTierFor(subtotal);
        if (amountTier != null) {
            instances.add(new Instance(amountTier, pctOf(subtotal, amountTier.getDiscountPercent()),
                    amountLabel(amountTier)));
        }

        if (paymentMethod != null) {
            for (Discount d : activeOfKind(Discount.Kind.PAGO)) {
                if (d.paymentMethodSet().contains(paymentMethod) && d.getDiscountPercent() > 0) {
                    instances.add(new Instance(d, pctOf(subtotal, d.getDiscountPercent()), pagoLabel(d)));
                }
            }
        }

        List<BreakdownLine> breakdown = new ArrayList<>();
        BigDecimal discountAmount;

        if (instances.isEmpty()) {
            discountAmount = BigDecimal.ZERO;
        } else if (instances.stream().allMatch(i -> i.discount().isStackable())) {
            // Todos acumulables → combinar en cascada (menor descuento total).
            discountAmount = BigDecimal.ZERO;
            BigDecimal remaining = subtotal;
            // orden: parámetro, monto, pago
            instances.sort(Comparator.comparingInt(i -> switch (i.discount().getKind()) {
                case PARAMETRO -> 0; case MONTO -> 1; default -> 2;
            }));
            for (Instance inst : instances) {
                // % efectivo del descuento sobre el subtotal, aplicado a lo que queda
                BigDecimal step = remaining
                        .multiply(inst.standaloneAmount())
                        .divide(subtotal, 0, RoundingMode.HALF_UP);
                if (step.signum() > 0) {
                    breakdown.add(new BreakdownLine(inst.breakdownLabel(), step, inst.discount().getDetail()));
                    discountAmount = discountAmount.add(step);
                    remaining = remaining.subtract(step);
                }
            }
        } else {
            // Hay al menos uno no acumulable → sólo el que más ahorra.
            Instance best = instances.stream()
                    .max(Comparator.comparing(Instance::standaloneAmount))
                    .orElseThrow();
            discountAmount = best.standaloneAmount();
            breakdown.add(new BreakdownLine(best.breakdownLabel(), discountAmount, best.discount().getDetail()));
        }

        discountAmount = discountAmount.min(subtotal);
        int discountPercent = discountAmount.signum() > 0
                ? discountAmount.multiply(BigDecimal.valueOf(100))
                    .divide(subtotal, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        return new CartDiscountResult(discountPercent, discountAmount, breakdown, freeShipping);
    }

    private FreeShipping freeShippingFor(BigDecimal subtotal, DeliveryMethod deliveryMethod) {
        if (deliveryMethod != DeliveryMethod.SHIPPING) return null;
        return activeOfKind(Discount.Kind.ENVIO_GRATIS).stream()
                .filter(d -> d.getMinAmount() != null && subtotal.compareTo(d.getMinAmount()) >= 0)
                .max(Comparator.comparing(Discount::getMinAmount))
                .map(d -> new FreeShipping(
                        d.getLabel() != null ? d.getLabel() : "Envío gratis", d.getDetail()))
                .orElse(null);
    }

    private Discount bestAmountTierFor(BigDecimal base) {
        return activeOfKind(Discount.Kind.MONTO).stream()
                .filter(d -> d.getMinAmount() != null && d.getMinAmount().signum() > 0)
                .filter(d -> base.compareTo(d.getMinAmount()) >= 0)
                .max(Comparator.comparing(Discount::getMinAmount))
                .orElse(null);
    }

    /** El próximo tier por monto todavía no alcanzado (para el banner del carrito). */
    @Transactional(readOnly = true)
    public Discount nextAmountTierFor(BigDecimal subtotal) {
        return activeOfKind(Discount.Kind.MONTO).stream()
                .filter(d -> d.getMinAmount() != null && d.getMinAmount().signum() > 0)
                .filter(d -> subtotal.compareTo(d.getMinAmount()) < 0)
                .min(Comparator.comparing(Discount::getMinAmount))
                .orElse(null);
    }

    private static String amountLabel(Discount d) {
        if (d.getLabel() != null) return d.getLabel();
        return "Compra mayor a $" + d.getMinAmount().toBigInteger() + " (" + d.getDiscountPercent() + "%)";
    }

    private static String pagoLabel(Discount d) {
        if (d.getLabel() != null) return d.getLabel();
        return "Descuento por medio de pago (" + d.getDiscountPercent() + "%)";
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

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
