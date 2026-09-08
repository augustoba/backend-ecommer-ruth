package com.estilospequenos.controller;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.dto.MetricsDtos.ComparisonResponse;
import com.estilospequenos.dto.MetricsDtos.MetricsResponse;
import com.estilospequenos.service.MetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Métricas de ventas del panel. Sólo admin (`/api/admin/**`). */
@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {

    private final MetricsService service;

    public MetricsController(MetricsService service) {
        this.service = service;
    }

    /**
     * @param from   inicio del período (inclusive). Por defecto: primer día de hace 11 meses.
     * @param to     fin del período (inclusive). Por defecto: hoy.
     * @param groupBy id del grupo de parametría para el desglose. Por defecto: "grp-tipo".
     */
    @GetMapping
    public MetricsResponse metrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {

        LocalDate today = LocalDate.now();
        LocalDate f = from != null ? from : today.withDayOfMonth(1).minusMonths(11);
        LocalDate t = to != null ? to : today;
        if (f.isAfter(t)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        return service.compute(f, t, groupBy);
    }

    /**
     * Comparativas del año: venta total mes a mes, y ventas del mismo tramo de
     * días (la semana en curso) mes a mes.
     *
     * @param year año a comparar. Por defecto: el actual.
     */
    @GetMapping("/comparison")
    public ComparisonResponse comparison(@RequestParam(required = false) Integer year) {
        return service.compareYear(year);
    }
}
