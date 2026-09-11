package com.estilospequenos.controller;

import com.estilospequenos.common.Csv;
import com.estilospequenos.model.Exchange;
import com.estilospequenos.model.ExchangeLine;
import com.estilospequenos.model.MarketingSend;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.SizeStock;
import com.estilospequenos.repository.MarketingSendRepository;
import com.estilospequenos.service.ExchangeService;
import com.estilospequenos.service.OrderService;
import com.estilospequenos.service.ProductService;
import com.estilospequenos.service.SupplierService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Exportaciones a CSV del panel (productos, pedidos, cambios). */
@RestController
@RequestMapping("/api/admin/export")
public class CsvExportController {

    private final ProductService products;
    private final OrderService orders;
    private final ExchangeService exchanges;
    private final SupplierService suppliers;
    private final MarketingSendRepository marketingSends;

    public CsvExportController(ProductService products, OrderService orders,
                              ExchangeService exchanges, SupplierService suppliers,
                              MarketingSendRepository marketingSends) {
        this.products = products;
        this.orders = orders;
        this.exchanges = exchanges;
        this.suppliers = suppliers;
        this.marketingSends = marketingSends;
    }

    @GetMapping("/products.csv")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public ResponseEntity<String> productsCsv() {
        var byId = new java.util.HashMap<String, String>();
        suppliers.findAll().forEach(s -> byId.put(s.getId(), s.getName()));

        Csv csv = Csv.withHeader("Nombre", "Precio", "Stock total", "Stock por talle",
                "Proveedor", "Costo", "Estado", "Rango de edad");
        for (Product p : products.findAll()) {
            int total = p.getSizeStocks().stream().mapToInt(SizeStock::getStock).sum();
            String perSize = p.getSizeStocks().stream()
                    .map(s -> s.getSize() + ":" + s.getStock())
                    .reduce((a, b) -> a + " " + b).orElse("");
            String estado = !p.isActive() ? "Oculto" : p.isDiscontinued() ? "No se repone" : "Publicado";
            csv.row(p.getName(), p.getPrice(), total, perSize,
                    p.getSupplierId() != null ? byId.getOrDefault(p.getSupplierId(), "") : "",
                    p.getCostPrice() != null ? p.getCostPrice() : "",
                    estado, p.getAgeRange());
        }
        return download(csv, "productos.csv");
    }

    @GetMapping("/orders.csv")
    @PreAuthorize("hasAuthority('ORDERS_VIEW')")
    public ResponseEntity<String> ordersCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        var page = orders.search(search, status, from, to,
                org.springframework.data.domain.PageRequest.of(0, 100_000));

        Csv csv = Csv.withHeader("Codigo", "Fecha", "Cliente", "Canal", "Estado", "Entrega",
                "Pago", "Subtotal", "Descuento", "Cupon", "Total", "Vendio", "Cobro", "Items");
        for (Order o : page.getContent()) {
            String items = o.getLines().stream()
                    .map(l -> l.getProductName() + " T" + l.getSize() + " x" + l.getQuantity())
                    .reduce((a, b) -> a + " | " + b).orElse("");
            csv.row(o.getCode(), o.getCreatedAt(), o.getCustomerName(),
                    o.getChannel(), o.getStatus(), o.getDeliveryMethod(),
                    o.getPaymentMethod() != null ? o.getPaymentMethod() : "",
                    o.getSubtotal(), o.getDiscountAmount(),
                    o.getCouponDiscount() != null ? o.getCouponDiscount() : "",
                    o.getTotal(),
                    o.getCreatedByName() != null ? o.getCreatedByName() : "",
                    o.getConfirmedByName() != null ? o.getConfirmedByName() : "",
                    items);
        }
        return download(csv, "pedidos.csv");
    }

    @GetMapping("/exchanges.csv")
    @PreAuthorize("hasAuthority('EXCHANGES_USE')")
    public ResponseEntity<String> exchangesCsv() {
        Csv csv = Csv.withHeader("Codigo", "Fecha", "Cliente", "Devuelve", "Se lleva",
                "Total devuelto", "Total llevado", "Diferencia", "Pago", "Nota", "Proceso");
        for (Exchange e : exchanges.findAll()) {
            String dev = e.getLines().stream()
                    .filter(l -> l.getKind() == ExchangeLine.Kind.DEVUELTA)
                    .map(l -> l.getProductName() + " T" + l.getSize() + " x" + l.getQuantity())
                    .reduce((a, b) -> a + " | " + b).orElse("");
            String llev = e.getLines().stream()
                    .filter(l -> l.getKind() == ExchangeLine.Kind.LLEVADA)
                    .map(l -> l.getProductName() + " T" + l.getSize() + " x" + l.getQuantity())
                    .reduce((a, b) -> a + " | " + b).orElse("");
            csv.row(e.getCode(), e.getCreatedAt(), e.getCustomerName(), dev, llev,
                    e.getReturnedTotal(), e.getTakenTotal(), e.getDifference(),
                    e.getPaymentMethod() != null ? e.getPaymentMethod() : "",
                    e.getNote() != null ? e.getNote() : "",
                    e.getProcessedByName() != null ? e.getProcessedByName() : "");
        }
        return download(csv, "cambios.csv");
    }

    @GetMapping("/marketing.csv")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public ResponseEntity<String> marketingCsv() {
        var page = marketingSends.search(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 100_000));

        Csv csv = Csv.withHeader("Fecha", "Email", "Motivo", "Cupon", "Estado", "Error");
        for (MarketingSend s : page.getContent()) {
            csv.row(s.getSentAt(), s.getEmail(), s.getReason(),
                    s.getCouponCode() != null ? s.getCouponCode() : "",
                    s.getStatus(), s.getErrorMessage() != null ? s.getErrorMessage() : "");
        }
        return download(csv, "marketing.csv");
    }

    private ResponseEntity<String> download(Csv csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.build());
    }
}
