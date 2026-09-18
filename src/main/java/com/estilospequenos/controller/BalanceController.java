package com.estilospequenos.controller;

import com.estilospequenos.dto.BalanceDtos.BalanceResponse;
import com.estilospequenos.dto.BalanceDtos.MonthBalance;
import com.estilospequenos.service.BalanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/balance")
@PreAuthorize("hasAuthority('FINANCE_VIEW')")
public class BalanceController {

    private final BalanceService service;

    public BalanceController(BalanceService service) {
        this.service = service;
    }

    @GetMapping
    public BalanceResponse get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.compute(from, to);
    }

    /** Serie mensual del año (para comparar mes a mes / año a año llamando dos veces desde el front). */
    @GetMapping("/comparison")
    public List<MonthBalance> comparison(@RequestParam(required = false) Integer year) {
        return service.compareYear(year != null ? year : LocalDate.now().getYear());
    }
}
