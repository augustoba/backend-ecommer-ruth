package com.estilospequenos.controller;

import com.estilospequenos.dto.CashRegisterDtos.CashRegisterResponse;
import com.estilospequenos.service.CashRegisterService;
import com.estilospequenos.service.ShiftService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/cash-register")
@PreAuthorize("hasAuthority('CASH_REGISTER_VIEW')")
public class CashRegisterController {

    private final CashRegisterService service;
    private final ShiftService shiftService;

    public CashRegisterController(CashRegisterService service, ShiftService shiftService) {
        this.service = service;
        this.shiftService = shiftService;
    }

    /** Cierre de caja del día (default: hoy). */
    @GetMapping
    public CashRegisterResponse forDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forDay(date);
    }

    /** Caja de un turno puntual (sólo lo que esa persona cobró/procesó en su ventana). */
    @GetMapping("/shift/{id}")
    public CashRegisterResponse forShift(@PathVariable String id) {
        return service.forShift(shiftService.get(id));
    }
}
