package com.estilospequenos.controller;

import com.estilospequenos.dto.ExpenseDtos.ExpenseRequest;
import com.estilospequenos.dto.ExpenseDtos.ExpenseResponse;
import com.estilospequenos.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/expenses")
@PreAuthorize("hasAuthority('EXPENSES_MANAGE')")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExpenseResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.search(from, to).stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable String id) {
        return ExpenseResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest req) {
        return ResponseEntity.status(201).body(ExpenseResponse.from(service.create(req)));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable String id, @Valid @RequestBody ExpenseRequest req) {
        return ExpenseResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
