package com.estilospequenos.controller;

import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LinesRequest;
import com.estilospequenos.dto.OrderDtos.OrderResponse;
import com.estilospequenos.dto.PageResponse;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Público: crea el pedido desde el carrito. */
    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.status(201).body(OrderResponse.from(service.create(req)));
    }

    // --- Admin ---

    /**
     * @param search texto libre: nombre del cliente o número/código de pedido
     * @param status PENDIENTE | PROCESADO | CANCELADO
     * @param from   fecha de creación desde (inclusive)
     * @param to     fecha de creación hasta (inclusive)
     */
    @GetMapping("/api/admin/orders")
    public PageResponse<OrderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        int capped = Math.min(Math.max(size, 1), 100);
        Page<com.estilospequenos.model.Order> result =
                service.search(search, status, from, to, PageRequest.of(Math.max(page, 0), capped));
        return PageResponse.of(result, result.map(OrderResponse::from).getContent());
    }

    @GetMapping("/api/admin/orders/pending-count")
    public Map<String, Long> pendingCount() {
        return Map.of("pending", service.pendingCount());
    }

    @GetMapping("/api/admin/orders/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(service.get(id));
    }

    @PutMapping("/api/admin/orders/{id}/lines")
    public OrderResponse setLines(@PathVariable String id, @Valid @RequestBody LinesRequest req) {
        return OrderResponse.from(service.setLineAcceptance(id, req.lines()));
    }

    @PostMapping("/api/admin/orders/{id}/confirm")
    public OrderResponse confirm(@PathVariable String id) {
        return OrderResponse.from(service.confirm(id));
    }

    @PostMapping("/api/admin/orders/{id}/cancel")
    public OrderResponse cancel(@PathVariable String id) {
        return OrderResponse.from(service.cancel(id));
    }
}
