package com.estilospequenos.controller;

import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LinesRequest;
import com.estilospequenos.dto.OrderDtos.OrderResponse;
import com.estilospequenos.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> list() {
        return service.findAll().stream().map(OrderResponse::from).toList();
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
