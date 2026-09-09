package com.estilospequenos.controller;

import com.estilospequenos.dto.CouponDtos.CouponCheckResponse;
import com.estilospequenos.dto.CouponDtos.CouponRequest;
import com.estilospequenos.dto.CouponDtos.CouponResponse;
import com.estilospequenos.dto.CouponDtos.EnabledPatch;
import com.estilospequenos.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    /** Público: valida un código de cupón para un subtotal (no lo consume). */
    @GetMapping("/api/coupons/{code}")
    public CouponCheckResponse check(@PathVariable String code,
                                     @RequestParam(defaultValue = "0") BigDecimal subtotal) {
        return service.check(code, subtotal);
    }

    // --- Admin ---

    @GetMapping("/api/admin/coupons")
    public List<CouponResponse> list() {
        return service.findAll().stream().map(CouponResponse::from).toList();
    }

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<List<CouponResponse>> create(@Valid @RequestBody CouponRequest req) {
        List<CouponResponse> created = service.create(req).stream().map(CouponResponse::from).toList();
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/api/admin/coupons/{id}")
    public CouponResponse update(@PathVariable String id, @Valid @RequestBody CouponRequest req) {
        return CouponResponse.from(service.update(id, req));
    }

    @PatchMapping("/api/admin/coupons/{id}/enabled")
    public CouponResponse setEnabled(@PathVariable String id, @Valid @RequestBody EnabledPatch body) {
        return CouponResponse.from(service.setEnabled(id, body.enabled()));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
