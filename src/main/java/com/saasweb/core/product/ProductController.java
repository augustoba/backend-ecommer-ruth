package com.saasweb.core.product;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.core.PageResponse;
import com.saasweb.core.product.ProductDtos.ActivePatch;
import com.saasweb.core.product.ProductDtos.DiscontinuedPatch;
import com.saasweb.core.product.ProductDtos.ProductRequest;
import com.saasweb.core.product.ProductDtos.ProductResponse;
import com.saasweb.core.product.ProductDtos.StockPatch;
import com.saasweb.core.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // --- Público (catálogo) ---

    @GetMapping("/api/products")
    public List<ProductResponse> publicList() {
        return service.findActive().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/api/products/best-sellers")
    public List<ProductResponse> bestSellers(@RequestParam(defaultValue = "8") int limit) {
        return service.bestSellers(Math.min(Math.max(limit, 1), 20)).stream()
                .map(ProductResponse::from).toList();
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse publicGet(@PathVariable String id) {
        return ProductResponse.from(service.get(id));
    }

    // --- Admin ---

    @GetMapping("/api/admin/products")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public PageResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String optionId,
            @RequestParam(defaultValue = "false") boolean noStock) {
        int capped = Math.min(Math.max(size, 1), 100);
        Page<com.saasweb.core.product.Product> result = service.search(
                search, supplierId, active, groupId, optionId, noStock,
                PageRequest.of(Math.max(page, 0), capped));
        return PageResponse.of(result, result.map(ProductResponse::from).getContent());
    }

    @GetMapping("/api/admin/products/archived")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public List<ProductResponse> archived() {
        return service.findArchived().stream().map(ProductResponse::from).toList();
    }

    @PostMapping("/api/admin/products/{id}/restore")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse restore(@PathVariable String id) {
        return ProductResponse.from(service.restore(id));
    }

    @GetMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(service.get(id));
    }

    /**
     * Para "cargar producto por código de barras" desde el panel: el admin
     * escanea un producto y, si ya existe, se lo manda a editar en vez de
     * cargarlo de nuevo. 404 si no hay ningún producto con ese código.
     */
    @GetMapping("/api/admin/products/by-barcode")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public ProductResponse byBarcode(@RequestParam String code) {
        Product product = service.findByBarcode(code);
        if (product == null) {
            throw new ResourceNotFoundException("No hay ningún producto con ese código de barras.");
        }
        return ProductResponse.from(product);
    }

    @PostMapping("/api/admin/products")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(201).body(ProductResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @PostMapping("/api/admin/products/{id}/duplicate")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ResponseEntity<ProductResponse> duplicate(@PathVariable String id) {
        return ResponseEntity.status(201).body(ProductResponse.from(service.duplicate(id)));
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/products/{id}/active")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse setActive(@PathVariable String id, @Valid @RequestBody ActivePatch body) {
        return ProductResponse.from(service.setActive(id, body.active()));
    }

    @PatchMapping("/api/admin/products/{id}/discontinued")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse setDiscontinued(@PathVariable String id, @Valid @RequestBody DiscontinuedPatch body) {
        return ProductResponse.from(service.setDiscontinued(id, body.discontinued()));
    }

    @PatchMapping("/api/admin/products/{id}/stock")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse setStock(@PathVariable String id, @Valid @RequestBody StockPatch body, Authentication auth) {
        return ProductResponse.from(service.setStock(id, body.size(), body.stock(), body.note(), auth.getName()));
    }

    @PostMapping("/api/admin/products/{id}/generate-barcode")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse generateBarcode(@PathVariable String id) {
        return ProductResponse.from(service.generateBarcode(id));
    }

    @PostMapping("/api/admin/products/{id}/purchases")
    @PreAuthorize("hasAuthority('PRODUCTS_MANAGE')")
    public ProductResponse registerPurchase(@PathVariable String id,
            @Valid @RequestBody com.saasweb.core.product.ProductDtos.PurchaseRequest req, Authentication auth) {
        return ProductResponse.from(service.registerPurchase(
                id, req.size(), req.quantity(), req.unitCost(), req.supplierId(), auth.getName()));
    }

    @GetMapping("/api/admin/stock-movements")
    @PreAuthorize("hasAuthority('STOCK_MOVEMENTS_VIEW')")
    public List<com.saasweb.core.product.ProductDtos.StockMovementResponse> movements(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.listMovements(productId, from, to).stream()
                .map(com.saasweb.core.product.ProductDtos.StockMovementResponse::from).toList();
    }
}
