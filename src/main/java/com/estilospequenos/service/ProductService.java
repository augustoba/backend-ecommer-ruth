package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.ProductDtos.ProductRequest;
import com.estilospequenos.dto.ProductDtos.SizeStockDto;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.ProductParam;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.SizeStock;
import com.estilospequenos.model.StockMovement;
import com.estilospequenos.model.StockMovementReason;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ProductRepository;
import com.estilospequenos.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final StockMovementRepository movementRepo;
    private final AdminUserRepository adminUsers;
    private final ZoneId zone = ZoneId.systemDefault();

    public ProductService(ProductRepository repo, OrderRepository orderRepo,
                          StockMovementRepository movementRepo, AdminUserRepository adminUsers) {
        this.repo = repo;
        this.orderRepo = orderRepo;
        this.movementRepo = movementRepo;
        this.adminUsers = adminUsers;
    }

    /** Resuelve el nombre a mostrar de un usuario del panel a partir de su DNI (mismo patrón que OrderService). */
    private String nameByDni(String dni) {
        if (dni == null || dni.isBlank()) return null;
        return adminUsers.findByDni(dni).map(u -> u.getNombre() + " " + u.getApellido()).orElse(null);
    }

    private void recordMovement(String productId, String productName, String size, int quantityDelta,
                                StockMovementReason reason, String note, String referenceId,
                                BigDecimal unitCost, String createdByDni) {
        if (quantityDelta == 0) return;
        StockMovement m = new StockMovement();
        m.setId(UUID.randomUUID().toString());
        m.setProductId(productId);
        m.setProductName(productName);
        m.setSize(size);
        m.setQuantityDelta(quantityDelta);
        m.setReason(reason);
        m.setNote(note);
        m.setReferenceId(referenceId);
        m.setUnitCost(unitCost);
        m.setCreatedByDni(createdByDni);
        m.setCreatedByName(nameByDni(createdByDni));
        movementRepo.save(m);
    }

    /** Historial de movimientos de un producto (o de todo el catálogo si `productId` es null), más nuevo primero. */
    @Transactional(readOnly = true)
    public List<StockMovement> listMovements(String productId, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return productId != null
                    ? movementRepo.findByProductIdOrderByCreatedAtDesc(productId)
                    : movementRepo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                            Instant.EPOCH, Instant.now().plus(1, ChronoUnit.DAYS));
        }
        Instant fromI = (from != null ? from : LocalDate.of(2000, 1, 1)).atStartOfDay(zone).toInstant();
        Instant toI = (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay(zone).toInstant();
        return productId != null
                ? movementRepo.findByProductIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        productId, fromI, toI)
                : movementRepo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(fromI, toI);
    }

    /**
     * Los más vendidos de los últimos 90 días (líneas aceptadas de pedidos
     * PROCESADO), sólo productos publicados y no archivados. Para la home.
     */
    @Transactional(readOnly = true)
    public List<Product> bestSellers(int limit) {
        Instant from = Instant.now().minus(90, ChronoUnit.DAYS);
        Map<String, Long> units = new LinkedHashMap<>();
        orderRepo.findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
                        OrderStatus.PROCESADO, from, Instant.now())
                .forEach(o -> o.getLines().forEach(l -> {
                    if (l.isAccepted()) units.merge(l.getProductId(), (long) l.getQuantity(), Long::sum);
                }));
        return units.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> repo.findById(e.getKey()).orElse(null))
                .filter(p -> p != null && p.isActive() && !p.isDeleted())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> findActive() {
        return repo.findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc();
    }

    /**
     * Portada (primera foto) de cada producto, resuelta en una sola consulta —
     * evita disparar el `@ElementCollection` lazy de `images` fila por fila
     * al armar el listado público (que sólo necesita `imageUrl`, no la
     * galería completa).
     */
    @Transactional(readOnly = true)
    public Map<String, String> coverImages(List<Product> products) {
        List<String> ids = products.stream().map(Product::getId).toList();
        if (ids.isEmpty()) return Map.of();
        Map<String, String> covers = new LinkedHashMap<>();
        for (com.estilospequenos.repository.ProductRepository.ProductCoverRow row : repo.findCoverImages(ids)) {
            covers.put(row.getProductId(), row.getUrl());
        }
        return covers;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repo.findByDeletedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findArchived() {
        return repo.findByDeletedTrueOrderByCreatedAtDesc();
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
        return repo.search(s, like, sup, active, gid, oid, noStock, pageable);
    }

    @Transactional(readOnly = true)
    public Product get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    /** Para buscar/escanear por código de barras en el panel. null si no hay ningún producto con ese código. */
    @Transactional(readOnly = true)
    public Product findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        return repo.findByBarcodeAndDeletedFalse(barcode.trim()).orElse(null);
    }

    /**
     * Genera un código interno para imprimir y pegar en la etiqueta — NO es un
     * EAN real (no hay autoridad emisora), sólo un código propio de esta
     * tienda para que el lector del POS lo reconozca. Alternativa/complemento
     * al QR (que siempre existe, no hace falta generarlo). No pisa un barcode
     * ya cargado.
     */
    public Product generateBarcode(String id) {
        Product p = get(id);
        if (p.getBarcode() != null && !p.getBarcode().isBlank()) {
            throw new BadRequestException("Este producto ya tiene un código de barras cargado.");
        }
        String digits = p.getId().replaceAll("\\D", "");
        String suffix = digits.length() >= 10
                ? digits.substring(0, 10)
                : String.format("%010d", Math.abs(p.getId().hashCode()));
        p.setBarcode("IN" + suffix);
        return repo.save(p);
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

    /**
     * Duplica un producto: copia todo salvo el stock (arranca en 0), lo deja
     * <b>oculto</b> (active=false) y le agrega " (copia)" al nombre. Sirve para
     * cargar variantes parecidas sin volver a tipear todo.
     */
    public Product duplicate(String id) {
        Product src = get(id);
        Product copy = new Product();
        copy.setId(UUID.randomUUID().toString());
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

    /**
     * Ajuste manual absoluto de stock (pisa el número). `note`/`createdByDni`
     * opcionales — quedan en el historial de movimientos (ver
     * {@link StockMovement}) como motivo del ajuste.
     */
    public Product setStock(String id, String size, int stock, String note, String createdByDni) {
        Product p = get(id);
        int clamped = Math.max(0, stock);
        int before = p.getSizeStocks().stream()
                .filter(s -> s.getSize().equals(size)).mapToInt(SizeStock::getStock).findFirst().orElse(0);
        boolean found = false;
        for (SizeStock s : p.getSizeStocks()) {
            if (s.getSize().equals(size)) {
                s.setStock(clamped);
                found = true;
            }
        }
        if (!found) p.getSizeStocks().add(new SizeStock(size, clamped));
        Product saved = repo.save(p);
        recordMovement(id, p.getName(), size, clamped - before, StockMovementReason.AJUSTE_MANUAL,
                note, null, null, createdByDni);
        return saved;
    }

    /** Descuenta unidades del stock de un talle puntual (venta, cambio que se lleva algo, etc). */
    public void decrementStock(String id, String size, int quantity, StockMovementReason reason,
                               String referenceId, String createdByDni) {
        repo.findById(id).ifPresent(p -> {
            int before = p.getSizeStocks().stream()
                    .filter(s -> s.getSize().equals(size)).mapToInt(SizeStock::getStock).findFirst().orElse(0);
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getSize().equals(size)) {
                    s.setStock(Math.max(0, s.getStock() - quantity));
                }
            }
            repo.save(p);
            int after = Math.max(0, before - quantity);
            recordMovement(id, p.getName(), size, after - before, reason, null, referenceId, null, createdByDni);
        });
    }

    /** Suma unidades al stock de un talle (cambio que se devuelve, compra a proveedor, etc). */
    public void incrementStock(String id, String size, int quantity, StockMovementReason reason,
                               String referenceId, BigDecimal unitCost, String createdByDni) {
        repo.findById(id).ifPresent(p -> {
            boolean found = false;
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getSize().equals(size)) {
                    s.setStock(Math.max(0, s.getStock() + quantity));
                    found = true;
                }
            }
            if (!found && quantity > 0) p.getSizeStocks().add(new SizeStock(size, quantity));
            repo.save(p);
            if (quantity > 0) {
                recordMovement(id, p.getName(), size, quantity, reason, null, referenceId, unitCost, createdByDni);
            }
        });
    }

    /**
     * Registra una compra a proveedor: suma stock y recalcula
     * {@code Product.costPrice} como el promedio ponderado entre el stock que
     * ya había (a su costo actual) y lo que entra (a su costo de compra) —
     * costeo por promedio ponderado, no FIFO por lote. Si el producto no
     * tenía costo cargado, el costo pasa a ser directo el de esta compra.
     */
    public Product registerPurchase(String id, String size, int quantity, BigDecimal unitCost,
                                    String supplierId, String createdByDni) {
        if (quantity <= 0) throw new BadRequestException("La cantidad tiene que ser mayor a 0.");
        if (unitCost == null || unitCost.signum() <= 0) {
            throw new BadRequestException("El costo unitario tiene que ser mayor a 0.");
        }
        Product p = get(id);
        int stockActual = p.getSizeStocks().stream().mapToInt(SizeStock::getStock).sum();
        BigDecimal costoActual = p.getCostPrice();
        BigDecimal nuevoCosto;
        if (costoActual == null || stockActual <= 0) {
            nuevoCosto = unitCost;
        } else {
            BigDecimal valorActual = costoActual.multiply(BigDecimal.valueOf(stockActual));
            BigDecimal valorCompra = unitCost.multiply(BigDecimal.valueOf(quantity));
            nuevoCosto = valorActual.add(valorCompra)
                    .divide(BigDecimal.valueOf(stockActual + quantity), 2, RoundingMode.HALF_UP);
        }
        p.setCostPrice(nuevoCosto);
        repo.save(p);
        incrementStock(id, size, quantity, StockMovementReason.ENTRADA_COMPRA, supplierId, unitCost, createdByDni);
        return get(id);
    }

    /** Stock actual de un talle puntual (0 si el producto no viene en ese talle). */
    @Transactional(readOnly = true)
    public int stockOf(String id, String size) {
        return repo.findById(id)
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
        p.setBarcode(blankToNull(req.barcode()));

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
