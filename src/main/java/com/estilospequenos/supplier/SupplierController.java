package com.estilospequenos.supplier;

import com.estilospequenos.supplier.SupplierDtos.SupplierRequest;
import com.estilospequenos.supplier.SupplierDtos.SupplierResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return service.findAll().stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable String id) {
        return SupplierResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest req) {
        return ResponseEntity.status(201).body(SupplierResponse.from(service.create(req)));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable String id, @Valid @RequestBody SupplierRequest req) {
        return SupplierResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
