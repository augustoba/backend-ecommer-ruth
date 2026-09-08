package com.estilospequenos.controller;

import com.estilospequenos.dto.SizeScaleDtos.ScaleRequest;
import com.estilospequenos.dto.SizeScaleDtos.ScaleResponse;
import com.estilospequenos.dto.SizeScaleDtos.ValuesRequest;
import com.estilospequenos.service.SizeScaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SizeScaleController {

    private final SizeScaleService service;

    public SizeScaleController(SizeScaleService service) {
        this.service = service;
    }

    @GetMapping("/api/size-scales")
    public List<ScaleResponse> publicList() {
        return service.findAll().stream().map(ScaleResponse::from).toList();
    }

    @GetMapping("/api/admin/size-scales")
    public List<ScaleResponse> list() {
        return service.findAll().stream().map(ScaleResponse::from).toList();
    }

    @PostMapping("/api/admin/size-scales")
    public ResponseEntity<ScaleResponse> create(@Valid @RequestBody ScaleRequest req) {
        return ResponseEntity.status(201).body(ScaleResponse.from(service.create(req)));
    }

    @PutMapping("/api/admin/size-scales/{id}")
    public ScaleResponse update(@PathVariable String id, @Valid @RequestBody ScaleRequest req) {
        return ScaleResponse.from(service.updateName(id, req));
    }

    @PutMapping("/api/admin/size-scales/{id}/values")
    public ScaleResponse replaceValues(@PathVariable String id, @Valid @RequestBody ValuesRequest req) {
        return ScaleResponse.from(service.replaceValues(id, req.values()));
    }

    @DeleteMapping("/api/admin/size-scales/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
