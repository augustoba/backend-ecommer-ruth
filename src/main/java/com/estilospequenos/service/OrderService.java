package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.DiscountDtos.CartDiscountResult;
import com.estilospequenos.dto.OrderDtos.CartItem;
import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LineAcceptance;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.ProductParam;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ProductRepository;
import com.estilospequenos.service.DiscountService.CartLineInput;
import com.estilospequenos.service.DiscountService;
import com.estilospequenos.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repo;
    private final ProductRepository productRepo;
    private final ProductService productService;
    private final DiscountService discountService;

    public OrderService(OrderRepository repo, ProductRepository productRepo,
                        ProductService productService, DiscountService discountService) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.productService = productService;
        this.discountService = discountService;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Pedido", id));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repo.countByStatus(OrderStatus.PENDIENTE);
    }

    /** Crea el pedido desde el carrito: totales + descuentos calculados server-side. */
    public Order create(CreateOrderRequest req) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setNumber(repo.maxNumber() + 1);
        order.setCustomerName(
                req.customerName() == null || req.customerName().isBlank()
                        ? "Sin nombre" : req.customerName().trim());
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.PENDIENTE);

        List<CartLineInput> discountInput = new ArrayList<>();
        for (CartItem item : req.items()) {
            Product p = productRepo.findById(item.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", item.productId()));

            OrderLine line = new OrderLine();
            line.setId(UUID.randomUUID().toString());
            line.setProductId(p.getId());
            line.setProductName(p.getName());
            line.setSize(item.size());
            line.setQuantity(item.quantity());
            line.setUnitPrice(p.getPrice());
            line.setAccepted(true);
            order.addLine(line);

            discountInput.add(new CartLineInput(p.getPrice(), item.quantity(), paramsOf(p)));
        }

        BigDecimal subtotal = order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDiscountResult discount = discountService.computeForLines(discountInput);

        order.setSubtotal(subtotal);
        order.setDiscountPercent(discount.discountPercent());
        order.setDiscountAmount(discount.discountAmount());
        order.setTotal(subtotal.subtract(discount.discountAmount()));

        return repo.save(order);
    }

    /** Tilda/destilda ítems (solo mientras el pedido está pendiente). */
    public Order setLineAcceptance(String orderId, List<LineAcceptance> changes) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue " + order.getStatus().name().toLowerCase() + ".");
        }
        Map<String, Boolean> byId = new LinkedHashMap<>();
        changes.forEach(c -> byId.put(c.lineId(), c.accepted()));
        for (OrderLine l : order.getLines()) {
            if (byId.containsKey(l.getId())) l.setAccepted(byId.get(l.getId()));
        }
        return repo.save(order);
    }

    /** Confirma: descuenta stock de las líneas aceptadas y marca PROCESADO. */
    public Order confirm(String orderId) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue procesado o cancelado.");
        }
        for (OrderLine l : order.getLines()) {
            if (l.isAccepted()) {
                productService.decrementStock(l.getProductId(), l.getSize(), l.getQuantity());
            }
        }
        order.setStatus(OrderStatus.PROCESADO);
        order.setProcessedAt(Instant.now());
        return repo.save(order);
    }

    /** Cancela el pedido completo sin tocar stock. */
    public Order cancel(String orderId) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue procesado o cancelado.");
        }
        order.setStatus(OrderStatus.CANCELADO);
        order.setProcessedAt(Instant.now());
        return repo.save(order);
    }

    private static Map<String, List<String>> paramsOf(Product p) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (ProductParam pp : p.getParams()) {
            map.computeIfAbsent(pp.getGroupId(), k -> new ArrayList<>()).add(pp.getOptionId());
        }
        return map;
    }
}
