package com.estilospequenos.product;

import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.product.ProductDtos.ProductRequest;
import com.estilospequenos.product.ProductDtos.SizeStockDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Product> findActive() {
        return repo.findByActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Product get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    public Product create(ProductRequest req) {
        Product p = new Product();
        p.setId(UUID.randomUUID().toString());
        p.setCreatedAt(Instant.now());
        apply(p, req);
        return repo.save(p);
    }

    public Product update(String id, ProductRequest req) {
        Product p = get(id);
        apply(p, req);
        return repo.save(p);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    public Product setActive(String id, boolean active) {
        Product p = get(id);
        p.setActive(active);
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
        repo.findById(id).ifPresent(p -> {
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getSize().equals(size)) {
                    s.setStock(Math.max(0, s.getStock() - quantity));
                }
            }
            repo.save(p);
        });
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description().trim());
        p.setPrice(req.price());
        p.setAgeRange(req.ageRange().trim());
        p.setImageUrl(req.imageUrl().trim());
        p.setActive(req.active() == null || req.active());
        p.setSizeScaleId(blankToNull(req.sizeScaleId()));
        p.setSupplierId(blankToNull(req.supplierId()));
        p.setCostPrice(req.costPrice() != null && req.costPrice().signum() > 0 ? req.costPrice() : null);

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
