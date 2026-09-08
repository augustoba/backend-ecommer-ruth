package com.estilospequenos.controller;

import com.estilospequenos.dto.DiscountDtos.ConfigRequest;
import com.estilospequenos.dto.DiscountDtos.ConfigResponse;
import com.estilospequenos.dto.DiscountDtos.DiscountRequest;
import com.estilospequenos.dto.DiscountDtos.DiscountResponse;
import com.estilospequenos.dto.DiscountDtos.PublicDiscounts;
import com.estilospequenos.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DiscountController {

    private final DiscountService service;

    public DiscountController(DiscountService service) {
        this.service = service;
    }

    /** Público: reglas de descuento para el preview del carrito. */
    @GetMapping("/api/discounts")
    public PublicDiscounts publicDiscounts() {
        return new PublicDiscounts(
                service.findAll().stream().map(DiscountResponse::from).toList(),
                service.getConfig().getCombineMode());
    }

    // --- Admin ---

    @GetMapping("/api/admin/discounts")
    public List<DiscountResponse> list() {
        return service.findAll().stream().map(DiscountResponse::from).toList();
    }

    @PostMapping("/api/admin/discounts")
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest req) {
        return ResponseEntity.status(201).body(DiscountResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/discounts/{id}")
    public DiscountResponse update(@PathVariable String id, @Valid @RequestBody DiscountRequest req) {
        return DiscountResponse.from(service.update(id, req));
    }

    @DeleteMapping("/api/admin/discounts/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/discounts/config")
    public ConfigResponse getConfig() {
        return ConfigResponse.from(service.getConfig());
    }

    @PutMapping("/api/admin/discounts/config")
    public ConfigResponse setConfig(@Valid @RequestBody ConfigRequest req) {
        return ConfigResponse.from(service.setConfig(req));
    }
}
