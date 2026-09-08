package com.estilospequenos.controller;

import com.estilospequenos.dto.ProductDtos.ActivePatch;
import com.estilospequenos.dto.ProductDtos.ProductRequest;
import com.estilospequenos.dto.ProductDtos.ProductResponse;
import com.estilospequenos.dto.ProductDtos.StockPatch;
import com.estilospequenos.service.ProductService;
import jakarta.validation.Valid;
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
    public List<ProductResponse> list() {
        return service.findAll().stream().map(ProductResponse::from).toList();
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

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/products/{id}/active")
    public ProductResponse setActive(@PathVariable String id, @Valid @RequestBody ActivePatch body) {
        return ProductResponse.from(service.setActive(id, body.active()));
    }

    @PatchMapping("/api/admin/products/{id}/stock")
    public ProductResponse setStock(@PathVariable String id, @Valid @RequestBody StockPatch body) {
        return ProductResponse.from(service.setStock(id, body.size(), body.stock()));
    }
}
