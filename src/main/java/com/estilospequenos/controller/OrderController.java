package com.estilospequenos.controller;

import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LinesRequest;
import com.estilospequenos.dto.OrderDtos.OrderResponse;
import com.estilospequenos.dto.OrderDtos.PublicOrderResponse;
import com.estilospequenos.dto.PageResponse;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    /** Público: consulta el estado de un pedido con el código + el nombre del cliente. */
    @GetMapping("/api/orders/lookup")
    public PublicOrderResponse lookup(@RequestParam String code, @RequestParam String name) {
        return PublicOrderResponse.from(service.lookup(code, name));
    }

    /** Venta armada en el local (POS): queda pendiente de cobro (ver {@link #confirm}). */
    @PostMapping("/api/admin/orders/pos")
    @PreAuthorize("hasAuthority('POS_USE')")
    public ResponseEntity<OrderResponse> createPos(@Valid @RequestBody CreateOrderRequest req, Authentication auth) {
        return ResponseEntity.status(201).body(OrderResponse.from(service.createPos(req, auth.getName())));
    }

    // --- Admin ---

    /**
     * @param search texto libre: nombre del cliente o número/código de pedido
     * @param status PENDIENTE | PROCESADO | CANCELADO
     * @param from   fecha de creación desde (inclusive)
     * @param to     fecha de creación hasta (inclusive)
     */
    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasAuthority('ORDERS_VIEW')")
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
    @PreAuthorize("hasAuthority('ORDERS_VIEW')")
    public Map<String, Long> pendingCount() {
        return Map.of("pending", service.pendingCount());
    }

    @GetMapping("/api/admin/orders/{id}")
    @PreAuthorize("hasAuthority('ORDERS_VIEW')")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(service.get(id));
    }

    @PutMapping("/api/admin/orders/{id}/lines")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse setLines(@PathVariable String id, @Valid @RequestBody LinesRequest req) {
        return OrderResponse.from(service.setLineAcceptance(id, req.lines()));
    }

    @PostMapping("/api/admin/orders/{id}/confirm")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse confirm(@PathVariable String id, Authentication auth) {
        return OrderResponse.from(service.confirm(id, auth.getName()));
    }

    @PostMapping("/api/admin/orders/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse cancel(@PathVariable String id) {
        return OrderResponse.from(service.cancel(id));
    }
}
