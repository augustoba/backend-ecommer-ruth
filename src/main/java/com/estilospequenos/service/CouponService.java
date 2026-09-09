package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.CouponDtos.CouponCheckResponse;
import com.estilospequenos.dto.CouponDtos.CouponRequest;
import com.estilospequenos.model.Coupon;
import com.estilospequenos.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CouponService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin I,O,0,1
    private final SecureRandom random = new SecureRandom();

    private final CouponRepository repo;

    public CouponService(CouponRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Coupon> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Coupon get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Cupón", id));
    }

    /** Crea uno o varios cupones (según `count`). Devuelve los creados. */
    public List<Coupon> create(CouponRequest req) {
        validateValue(req);
        int count = req.count() != null && req.count() > 1 ? req.count() : 1;

        List<Coupon> created = new ArrayList<>();
        if (count == 1) {
            String code = req.code() != null && !req.code().isBlank()
                    ? normalize(req.code())
                    : generateCode(req.codePrefix());
            if (repo.existsByCodeIgnoreCase(code)) {
                throw new BadRequestException("Ya existe un cupón con el código " + code + ".");
            }
            created.add(repo.save(build(code, req)));
        } else {
            for (int i = 0; i < count; i++) {
                String code;
                do {
                    code = generateCode(req.codePrefix());
                } while (repo.existsByCodeIgnoreCase(code));
                created.add(repo.save(build(code, req)));
            }
        }
        return created;
    }

    public Coupon update(String id, CouponRequest req) {
        validateValue(req);
        Coupon c = get(id);
        if (req.code() != null && !req.code().isBlank()) {
            String code = normalize(req.code());
            if (!code.equalsIgnoreCase(c.getCode()) && repo.existsByCodeIgnoreCase(code)) {
                throw new BadRequestException("Ya existe un cupón con el código " + code + ".");
            }
            c.setCode(code);
        }
        c.setKind(req.kind());
        c.setValue(req.value());
        c.setMinAmount(req.minAmount() != null && req.minAmount().signum() > 0 ? req.minAmount() : null);
        c.setMaxUses(req.maxUses());
        c.setExpiresAt(req.expiresAt());
        c.setEnabled(req.enabled() == null || req.enabled());
        c.setStackable(req.stackable() == null || req.stackable());
        c.setLabel(req.label() != null && !req.label().isBlank() ? req.label().trim() : null);
        return repo.save(c);
    }

    public Coupon setEnabled(String id, boolean enabled) {
        Coupon c = get(id);
        c.setEnabled(enabled);
        return repo.save(c);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    // --- Uso en el carrito ---

    /** Valida un código para un subtotal dado. No lo consume. Tira 400 con el motivo si no sirve. */
    @Transactional(readOnly = true)
    public CouponCheckResponse check(String code, BigDecimal subtotal) {
        Coupon c = repo.findByCodeIgnoreCase(normalize(code))
                .orElseThrow(() -> new BadRequestException("El cupón no existe."));
        assertUsable(c, subtotal);
        BigDecimal amount = discountFor(c, subtotal);
        return new CouponCheckResponse(c.getCode(), c.getKind(), c.getValue(), amount,
                c.isStackable(), c.getLabel());
    }

    /** Cupón consumido: código normalizado, monto de descuento y si combina con promos. */
    public record Redemption(String code, BigDecimal amount, boolean stackable) {}

    /**
     * Consume un uso del cupón si es válido y devuelve el descuento sobre el
     * subtotal. Se llama al crear el pedido. Tira 400 con el motivo si no sirve.
     */
    public Redemption redeem(String code, BigDecimal subtotal) {
        Coupon c = repo.findByCodeIgnoreCase(normalize(code))
                .orElseThrow(() -> new BadRequestException("El cupón no existe."));
        assertUsable(c, subtotal);
        c.setUsedCount(c.getUsedCount() + 1);
        repo.save(c);
        return new Redemption(c.getCode(), discountFor(c, subtotal), c.isStackable());
    }

    @Transactional(readOnly = true)
    public Coupon findByCode(String code) {
        return repo.findByCodeIgnoreCase(normalize(code)).orElse(null);
    }

    private void assertUsable(Coupon c, BigDecimal subtotal) {
        if (!c.isEnabled()) throw new BadRequestException("El cupón está deshabilitado.");
        if (c.isExpired()) throw new BadRequestException("El cupón está vencido.");
        if (c.isExhausted()) throw new BadRequestException("El cupón ya se usó.");
        if (c.getMinAmount() != null && subtotal.compareTo(c.getMinAmount()) < 0) {
            throw new BadRequestException("El cupón necesita una compra mínima de $"
                    + c.getMinAmount().toBigInteger() + ".");
        }
    }

    private BigDecimal discountFor(Coupon c, BigDecimal subtotal) {
        BigDecimal amount = c.getKind() == Coupon.Kind.PERCENT
                ? subtotal.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : c.getValue();
        return amount.min(subtotal).max(BigDecimal.ZERO);
    }

    private Coupon build(String code, CouponRequest req) {
        Coupon c = new Coupon();
        c.setId(UUID.randomUUID().toString());
        c.setCode(code);
        c.setKind(req.kind());
        c.setValue(req.value());
        c.setMinAmount(req.minAmount() != null && req.minAmount().signum() > 0 ? req.minAmount() : null);
        c.setMaxUses(req.maxUses());
        c.setExpiresAt(req.expiresAt());
        c.setEnabled(req.enabled() == null || req.enabled());
        c.setStackable(req.stackable() == null || req.stackable());
        c.setLabel(req.label() != null && !req.label().isBlank() ? req.label().trim() : null);
        return c;
    }

    private void validateValue(CouponRequest req) {
        if (req.value() == null || req.value().signum() <= 0) {
            throw new BadRequestException("El descuento del cupón tiene que ser mayor a 0.");
        }
        if (req.kind() == Coupon.Kind.PERCENT && req.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje no puede ser mayor a 100.");
        }
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private String generateCode(String prefix) {
        StringBuilder sb = new StringBuilder();
        String p = prefix != null ? normalize(prefix) : "";
        if (!p.isEmpty()) sb.append(p).append("-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
