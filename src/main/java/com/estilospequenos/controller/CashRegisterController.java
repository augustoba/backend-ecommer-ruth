package com.estilospequenos.controller;

import com.estilospequenos.dto.CashRegisterDtos.CashRegisterResponse;
import com.estilospequenos.service.CashRegisterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/cash-register")
@PreAuthorize("hasAuthority('CASH_REGISTER_VIEW')")
public class CashRegisterController {

    private final CashRegisterService service;

    public CashRegisterController(CashRegisterService service) {
        this.service = service;
    }

    /** Cierre de caja del día (default: hoy). */
    @GetMapping
    public CashRegisterResponse forDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forDay(date);
    }
}
