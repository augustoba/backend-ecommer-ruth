package com.estilospequenos.controller;

import com.estilospequenos.dto.DashboardDtos.DashboardResponse;
import com.estilospequenos.dto.DashboardDtos.LowStockItem;
import com.estilospequenos.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Resumen del panel de administración. Sólo admin (`/api/admin/**`). */
@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardResponse dashboard() {
        return service.get();
    }

    /** Sólo la lista de talles por reponer (para el badge del menú y la pantalla de stock). */
    @GetMapping("/api/admin/low-stock")
    public List<LowStockItem> lowStock() {
        return service.lowStock();
    }
}
