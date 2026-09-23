package com.estilospequenos.controller;

import com.estilospequenos.dto.OrderDtos.AddLineRequest;
import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LineIdsRequest;
import com.estilospequenos.dto.OrderDtos.LineQuantityRequest;
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

    /** Público: crea el pedido desde el carrito (si el medio es Mercado Pago, arranca el checkout online). */
    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.status(201).body(OrderResponse.from(service.createWebCheckout(req)));
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

    /** Entrega parcial: confirma sólo las líneas indicadas, deja el resto pendiente. */
    @PostMapping("/api/admin/orders/{id}/confirm-lines")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse confirmLines(@PathVariable String id, @Valid @RequestBody LineIdsRequest req, Authentication auth) {
        return OrderResponse.from(service.confirmLines(id, req.lineIds(), auth.getName()));
    }

    /** Cancela sólo las líneas indicadas, deja el resto pendiente. */
    @PostMapping("/api/admin/orders/{id}/cancel-lines")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse cancelLines(@PathVariable String id, @Valid @RequestBody LineIdsRequest req) {
        return OrderResponse.from(service.cancelLines(id, req.lineIds()));
    }

    /** Agrega un ítem a un pedido todavía pendiente. */
    @PostMapping("/api/admin/orders/{id}/lines")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse addLine(@PathVariable String id, @Valid @RequestBody AddLineRequest req) {
        return OrderResponse.from(service.addLine(id, req.productId(), req.size(), req.quantity()));
    }

    /** Cambia la cantidad de una línea todavía pendiente. */
    @PatchMapping("/api/admin/orders/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse updateLineQuantity(@PathVariable String id, @PathVariable String lineId,
                                             @Valid @RequestBody LineQuantityRequest req) {
        return OrderResponse.from(service.updateLineQuantity(id, lineId, req.quantity()));
    }

    /** Saca una línea todavía pendiente del pedido. */
    @DeleteMapping("/api/admin/orders/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('ORDERS_MANAGE')")
    public OrderResponse removeLine(@PathVariable String id, @PathVariable String lineId) {
        return OrderResponse.from(service.removeLine(id, lineId));
    }
}
