package com.estilospequenos.controller;

import com.estilospequenos.dto.ExchangeDtos.CreateExchangeRequest;
import com.estilospequenos.dto.ExchangeDtos.ExchangeResponse;
import com.estilospequenos.service.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exchanges")
@PreAuthorize("hasAuthority('POS_USE')")
public class ExchangeController {

    private final ExchangeService service;

    public ExchangeController(ExchangeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExchangeResponse> list() {
        return service.findAll().stream().map(ExchangeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ExchangeResponse get(@PathVariable String id) {
        return ExchangeResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> create(@Valid @RequestBody CreateExchangeRequest req) {
        return ResponseEntity.status(201).body(ExchangeResponse.from(service.create(req)));
    }
}
