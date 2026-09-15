package com.saasweb.core.product;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.product.ProductDtos.ProductRequest;
import com.saasweb.core.product.ProductDtos.SizeStockDto;
import com.saasweb.core.product.Product;
import com.saasweb.core.product.ProductParam;
import com.saasweb.core.order.OrderStatus;
import com.saasweb.modules.ropa.SizeStock;
import com.saasweb.core.order.OrderRepository;
import com.saasweb.core.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repo;
    private final OrderRepository orderRepo;

    public ProductService(ProductRepository repo, OrderRepository orderRepo) {
        this.repo = repo;
        this.orderRepo = orderRepo;
    }

    /**
     * Los más vendidos de los últimos 90 días (líneas aceptadas de pedidos
     * PROCESADO), sólo productos publicados y no archivados. Para la home.
     */
    @Transactional(readOnly = true)
    public List<Product> bestSellers(int limit) {
        String tenantId = TenantContext.getTenantId();
        Instant from = Instant.now().minus(90, ChronoUnit.DAYS);
        Map<String, Long> units = new LinkedHashMap<>();
        orderRepo.findByTenantIdAndStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
                        tenantId, OrderStatus.PROCESADO, from, Instant.now())
                .forEach(o -> o.getLines().forEach(l -> {
                    if (l.isAccepted()) units.merge(l.getProductId(), (long) l.getQuantity(), Long::sum);
                }));
        return units.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> repo.findByIdAndTenantId(e.getKey(), tenantId).orElse(null))
                .filter(p -> p != null && p.isActive() && !p.isDeleted())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> findActive() {
        return repo.findByTenantIdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repo.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public List<Product> findArchived() {
        return repo.findByTenantIdAndDeletedTrueOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    /** Listado del panel con filtros (ver ProductRepository.search). */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Product> search(
            String search, String supplierId, Boolean active,
            String groupId, String optionId, boolean noStock,
            org.springframework.data.domain.Pageable pageable) {
        String s = (search != null && !search.isBlank()) ? search.trim() : null;
        String like = s != null ? "%" + s.toLowerCase() + "%" : null;
        String sup = (supplierId != null && !supplierId.isBlank()) ? supplierId.trim() : null;
        String gid = (groupId != null && !groupId.isBlank()) ? groupId.trim() : null;
        String oid = (optionId != null && !optionId.isBlank()) ? optionId.trim() : null;
        // el filtro de parametría necesita el par completo
        if (gid == null || oid == null) { gid = null; oid = null; }
        return repo.search(TenantContext.getTenantId(), s, like, sup, active, gid, oid, noStock, pageable);
    }

    @Transactional(readOnly = true)
    public Product get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    public Product create(ProductRequest req) {
        Product p = new Product();
        p.setId(UUID.randomUUID().toString());
        p.setTenantId(TenantContext.getTenantId());
        p.setCreatedAt(Instant.now());
        apply(p, req);
        return repo.save(p);
    }

    public Product update(String id, ProductRequest req) {
        Product p = get(id);
        apply(p, req);
        return repo.save(p);
    }

    /**
     * Duplica un producto: copia todo salvo el stock (arranca en 0), lo deja
     * <b>oculto</b> (active=false) y le agrega " (copia)" al nombre. Sirve para
     * cargar variantes parecidas sin volver a tipear todo.
     */
    public Product duplicate(String id) {
        Product src = get(id);
        Product copy = new Product();
        copy.setId(UUID.randomUUID().toString());
        copy.setTenantId(src.getTenantId());
        copy.setCreatedAt(Instant.now());
        copy.setName(src.getName() + " (copia)");
        copy.setDescription(src.getDescription());
        copy.setPrice(src.getPrice());
        copy.setAgeRange(src.getAgeRange());
        copy.getImages().addAll(src.getImages());
        copy.setVideoUrl(src.getVideoUrl());
        copy.setActive(false);
        copy.setDiscontinued(false);
        copy.setSizeScaleId(src.getSizeScaleId());
        copy.setSupplierId(src.getSupplierId());
        copy.setCostPrice(src.getCostPrice());
        copy.setLowStockThreshold(src.getLowStockThreshold());
        for (SizeStock s : src.getSizeStocks()) {
            copy.getSizeStocks().add(new SizeStock(s.getSize(), 0));
        }
        for (ProductParam pp : src.getParams()) {
            copy.getParams().add(new ProductParam(pp.getGroupId(), pp.getOptionId()));
        }
        return repo.save(copy);
    }

    /** Soft-delete: archiva el producto (lo saca de todos lados) sin borrar la fila. */
    public void delete(String id) {
        Product p = get(id);
        p.setDeleted(true);
        p.setActive(false);
        repo.save(p);
    }

    /** Restaura un producto archivado (queda oculto: hay que republicarlo a mano). */
    public Product restore(String id) {
        Product p = get(id);
        p.setDeleted(false);
        return repo.save(p);
    }

    public Product setActive(String id, boolean active) {
        Product p = get(id);
        p.setActive(active);
        return repo.save(p);
    }

    public Product setDiscontinued(String id, boolean discontinued) {
        Product p = get(id);
        p.setDiscontinued(discontinued);
        return repo.save(p);
    }

    public Product setStock(String id, String size, int stock) {
        Product p = get(id);
        int clamped = Math.max(0, stock);
        boolean found = false;
        for (SizeStock s : p.getSizeStocks()) {
            if (s.getSize().equals(size)) {
                s.setStock(clamped);
                found = true;
            }
        }
        if (!found) p.getSizeStocks().add(new SizeStock(size, clamped));
        return repo.save(p);
    }

    /** Descuenta unidades del stock de un talle puntual (al confirmar un pedido). */
    public void decrementStock(String id, String size, int quantity) {
        repo.findByIdAndTenantId(id, TenantContext.getTenantId()).ifPresent(p -> {
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getSize().equals(size)) {
                    s.setStock(Math.max(0, s.getStock() - quantity));
                }
            }
            repo.save(p);
        });
    }

    /** Suma unidades al stock de un talle (ej: prenda devuelta en un cambio). */
    public void incrementStock(String id, String size, int quantity) {
        repo.findByIdAndTenantId(id, TenantContext.getTenantId()).ifPresent(p -> {
            boolean found = false;
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getSize().equals(size)) {
                    s.setStock(Math.max(0, s.getStock() + quantity));
                    found = true;
                }
            }
            if (!found && quantity > 0) p.getSizeStocks().add(new SizeStock(size, quantity));
            repo.save(p);
        });
    }

    /** Stock actual de un talle puntual (0 si el producto no viene en ese talle). */
    @Transactional(readOnly = true)
    public int stockOf(String id, String size) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .map(p -> p.getSizeStocks().stream()
                        .filter(s -> s.getSize().equals(size))
                        .mapToInt(SizeStock::getStock)
                        .findFirst().orElse(0))
                .orElse(0);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description().trim());
        p.setPrice(req.price());
        p.setAgeRange(req.ageRange().trim());

        p.getImages().clear();
        if (req.images() != null) {
            for (String url : req.images()) {
                if (url != null && !url.isBlank()) p.getImages().add(url.trim());
            }
        }

        p.setVideoUrl(blankToNull(req.videoUrl()));
        p.setActive(req.active() == null || req.active());
        p.setDiscontinued(req.discontinued() != null && req.discontinued());
        p.setSizeScaleId(blankToNull(req.sizeScaleId()));
        p.setSupplierId(blankToNull(req.supplierId()));
        p.setCostPrice(req.costPrice() != null && req.costPrice().signum() > 0 ? req.costPrice() : null);
        p.setLowStockThreshold(
                req.lowStockThreshold() != null && req.lowStockThreshold() >= 0 ? req.lowStockThreshold() : null);

        p.getSizeStocks().clear();
        if (req.sizeStocks() != null) {
            for (SizeStockDto s : req.sizeStocks()) {
                p.getSizeStocks().add(new SizeStock(s.size(), Math.max(0, s.stock())));
            }
        }

        p.getParams().clear();
        if (req.params() != null) {
            Set<ProductParam> next = new LinkedHashSet<>();
            for (Map.Entry<String, List<String>> e : req.params().entrySet()) {
                List<String> opts = e.getValue() == null ? List.of() : e.getValue();
                for (String opt : opts) {
                    if (opt != null && !opt.isBlank()) {
                        next.add(new ProductParam(e.getKey(), opt));
                    }
                }
            }
            p.getParams().addAll(next);
        }
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // --- helpers usados por el catálogo/seed ---

    public static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
