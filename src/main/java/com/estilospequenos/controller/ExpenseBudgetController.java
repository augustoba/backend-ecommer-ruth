package com.estilospequenos.controller;

import com.estilospequenos.dto.ExpenseBudgetDtos.BudgetRequest;
import com.estilospequenos.dto.ExpenseBudgetDtos.BudgetResponse;
import com.estilospequenos.dto.ExpenseBudgetDtos.BudgetStatus;
import com.estilospequenos.service.ExpenseBudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/expense-budgets")
@PreAuthorize("hasAuthority('EXPENSES_MANAGE')")
public class ExpenseBudgetController {

    private final ExpenseBudgetService service;

    public ExpenseBudgetController(ExpenseBudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetResponse> list() {
        return service.findAll().stream().map(BudgetResponse::from).toList();
    }

    @GetMapping("/status")
    public List<BudgetStatus> status() {
        return service.statusForCurrentMonth();
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> upsert(@Valid @RequestBody BudgetRequest req) {
        return ResponseEntity.status(201).body(BudgetResponse.from(service.upsert(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
