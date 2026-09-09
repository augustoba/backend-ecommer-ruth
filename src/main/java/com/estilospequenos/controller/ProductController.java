package com.estilospequenos.controller;

import com.estilospequenos.dto.PageResponse;
import com.estilospequenos.dto.ProductDtos.ActivePatch;
import com.estilospequenos.dto.ProductDtos.DiscontinuedPatch;
import com.estilospequenos.dto.ProductDtos.ProductRequest;
import com.estilospequenos.dto.ProductDtos.ProductResponse;
import com.estilospequenos.dto.ProductDtos.StockPatch;
import com.estilospequenos.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/api/products/{id}")
    public ProductResponse publicGet(@PathVariable String id) {
        return ProductResponse.from(service.get(id));
    }

    // --- Admin ---

    @GetMapping("/api/admin/products")
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
        Page<com.estilospequenos.model.Product> result = service.search(
                search, supplierId, active, groupId, optionId, noStock,
                PageRequest.of(Math.max(page, 0), capped));
        return PageResponse.of(result, result.map(ProductResponse::from).getContent());
    }

    @GetMapping("/api/admin/products/archived")
    public List<ProductResponse> archived() {
        return service.findArchived().stream().map(ProductResponse::from).toList();
    }

    @PostMapping("/api/admin/products/{id}/restore")
    public ProductResponse restore(@PathVariable String id) {
        return ProductResponse.from(service.restore(id));
    }

    @GetMapping("/api/admin/products/{id}")
    public ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(service.get(id));
    }

    @PostMapping("/api/admin/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(201).body(ProductResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/products/{id}")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @PostMapping("/api/admin/products/{id}/duplicate")
    public ResponseEntity<ProductResponse> duplicate(@PathVariable String id) {
        return ResponseEntity.status(201).body(ProductResponse.from(service.duplicate(id)));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/products/{id}/active")
    public ProductResponse setActive(@PathVariable String id, @Valid @RequestBody ActivePatch body) {
        return ProductResponse.from(service.setActive(id, body.active()));
    }

    @PatchMapping("/api/admin/products/{id}/discontinued")
    public ProductResponse setDiscontinued(@PathVariable String id, @Valid @RequestBody DiscontinuedPatch body) {
        return ProductResponse.from(service.setDiscontinued(id, body.discontinued()));
    }

    @PatchMapping("/api/admin/products/{id}/stock")
    public ProductResponse setStock(@PathVariable String id, @Valid @RequestBody StockPatch body) {
        return ProductResponse.from(service.setStock(id, body.size(), body.stock()));
    }
}
