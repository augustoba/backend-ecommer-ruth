package com.estilospequenos.discount;

import com.estilospequenos.discount.DiscountDtos.ConfigRequest;
import com.estilospequenos.discount.DiscountDtos.ConfigResponse;
import com.estilospequenos.discount.DiscountDtos.DiscountRequest;
import com.estilospequenos.discount.DiscountDtos.DiscountResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discounts")
public class DiscountController {

    private final DiscountService service;

    public DiscountController(DiscountService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiscountResponse> list() {
        return service.findAll().stream().map(DiscountResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest req) {
        return ResponseEntity.status(201).body(DiscountResponse.from(service.create(req)));
    }

    @PutMapping("/{id}")
    public DiscountResponse update(@PathVariable String id, @Valid @RequestBody DiscountRequest req) {
        return DiscountResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/config")
    public ConfigResponse getConfig() {
        return ConfigResponse.from(service.getConfig());
    }

    @PutMapping("/config")
    public ConfigResponse setConfig(@Valid @RequestBody ConfigRequest req) {
        return ConfigResponse.from(service.setConfig(req));
    }
}
