package com.estilospequenos.controller;

import com.estilospequenos.dto.SizeScaleDtos.ScaleRequest;
import com.estilospequenos.dto.SizeScaleDtos.ScaleResponse;
import com.estilospequenos.dto.SizeScaleDtos.ValuesRequest;
import com.estilospequenos.service.SizeScaleService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class SizeScaleController {

    private final SizeScaleService service;

    public SizeScaleController(SizeScaleService service) {
        this.service = service;
    }

    /** Público. Cambia poco: cacheable 5 min. */
    @GetMapping("/api/size-scales")
    public ResponseEntity<List<ScaleResponse>> publicList() {
        List<ScaleResponse> body = service.findAll().stream().map(ScaleResponse::from).toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(body);
    }

    @GetMapping("/api/admin/size-scales")
    public List<ScaleResponse> list() {
        return service.findAll().stream().map(ScaleResponse::from).toList();
    }

    @PostMapping("/api/admin/size-scales")
    @PreAuthorize("hasAuthority('SIZE_SCALES_MANAGE')")
    public ResponseEntity<ScaleResponse> create(@Valid @RequestBody ScaleRequest req) {
        return ResponseEntity.status(201).body(ScaleResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/size-scales/{id}")
    @PreAuthorize("hasAuthority('SIZE_SCALES_MANAGE')")
    public ScaleResponse update(@PathVariable String id, @Valid @RequestBody ScaleRequest req) {
        return ScaleResponse.from(service.updateName(id, req));
    }

    @PutMapping("/api/admin/size-scales/{id}/values")
    @PreAuthorize("hasAuthority('SIZE_SCALES_MANAGE')")
    public ScaleResponse replaceValues(@PathVariable String id, @Valid @RequestBody ValuesRequest req) {
        return ScaleResponse.from(service.replaceValues(id, req.values()));
    }

    @DeleteMapping("/api/admin/size-scales/{id}")
    @PreAuthorize("hasAuthority('SIZE_SCALES_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
