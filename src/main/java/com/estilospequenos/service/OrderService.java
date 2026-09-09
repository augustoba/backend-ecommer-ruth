package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.DiscountDtos.CartDiscountResult;
import com.estilospequenos.dto.OrderDtos.CartItem;
import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.dto.OrderDtos.LineAcceptance;
import com.estilospequenos.model.DeliveryMethod;
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
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final CouponService couponService;

    public OrderService(OrderRepository repo, ProductRepository productRepo,
                        ProductService productService, DiscountService discountService,
                        CouponService couponService) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.productService = productService;
        this.discountService = discountService;
        this.couponService = couponService;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> findAll(org.springframework.data.domain.Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDesc(pageable);
    }

    private final ZoneId zone = ZoneId.systemDefault();

    /**
     * Listado del panel con filtros opcionales: `search` (nombre del cliente o
     * número/código de pedido), `status`, y `from`/`to` sobre la fecha de creación.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> search(
            String search, OrderStatus status, LocalDate from, LocalDate to,
            org.springframework.data.domain.Pageable pageable) {

        Instant fromI = from != null ? from.atStartOfDay(zone).toInstant() : null;
        Instant toI = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;

        String s = (search != null && !search.isBlank()) ? search.trim() : null;
        String like = s != null ? "%" + s.toLowerCase() + "%" : null;
        long num = -1;
        if (s != null) {
            String digits = s.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                try { num = Long.parseLong(digits); } catch (NumberFormatException ignored) { /* -1 */ }
            }
        }
        return repo.search(status, fromI, toI, s, like, num, pageable);
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Pedido", id));
    }

    /**
     * Consulta pública de un pedido por su código + nombre del cliente. Pide que
     * el nombre coincida (ignora mayúsculas/espacios) para que no se puedan
     * enumerar pedidos ajenos con sólo el código correlativo.
     */
    @Transactional(readOnly = true)
    public Order lookup(String code, String name) {
        String digits = code == null ? "" : code.replaceAll("\\D", "");
        if (digits.isEmpty() || name == null || name.isBlank()) {
            throw ResourceNotFoundException.of("Pedido", code);
        }
        long number;
        try {
            number = Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw ResourceNotFoundException.of("Pedido", code);
        }
        return repo.findByNumber(number)
                .filter(o -> o.getCustomerName().trim().equalsIgnoreCase(name.trim()))
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", code));
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

        DeliveryMethod delivery = req.deliveryMethod() != null ? req.deliveryMethod() : DeliveryMethod.PICKUP;
        order.setDeliveryMethod(delivery);
        order.setPaymentMethod(req.paymentMethod());
        if (delivery == DeliveryMethod.SHIPPING) {
            String addr = req.shippingAddress() != null ? req.shippingAddress().trim() : "";
            if (addr.isEmpty()) {
                throw new BadRequestException("Elegiste envío a domicilio: falta la dirección.");
            }
            order.setShippingAddress(addr);
            order.setShippingReference(blankToNull(req.shippingReference()));
            order.setShippingLat(req.shippingLat());
            order.setShippingLng(req.shippingLng());
        }

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

        CartDiscountResult discount =
                discountService.computeForLines(discountInput, order.getPaymentMethod(), delivery);

        BigDecimal autoDiscount = discount.discountAmount();
        BigDecimal couponDiscount = BigDecimal.ZERO;

        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            // check() valida (existe / vigente / mínimo) sin consumir.
            var chk = couponService.check(req.couponCode(), subtotal);
            boolean couponWins = chk.stackable() || autoDiscount.signum() == 0
                    || chk.discountAmount().compareTo(autoDiscount) >= 0;
            if (couponWins) {
                // Recién acá se consume el uso.
                couponDiscount = couponService.redeem(req.couponCode(), subtotal).amount();
                order.setCouponCode(chk.code());
                if (!chk.stackable()) autoDiscount = BigDecimal.ZERO;
            }
        }

        // El total no puede bajar de 0: si los dos descuentos se pasan, se recorta el del cupón.
        if (autoDiscount.add(couponDiscount).compareTo(subtotal) > 0) {
            couponDiscount = subtotal.subtract(autoDiscount).max(BigDecimal.ZERO);
        }

        order.setSubtotal(subtotal);
        order.setDiscountPercent(discount.discountPercent());
        order.setDiscountAmount(autoDiscount);
        order.setCouponDiscount(couponDiscount.signum() > 0 ? couponDiscount : null);
        order.setTotal(subtotal.subtract(autoDiscount).subtract(couponDiscount));
        String notes = discount.breakdown().stream()
                .map(com.estilospequenos.dto.DiscountDtos.BreakdownLine::detail)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(null);
        order.setDiscountNote(notes);
        if (discount.freeShipping() != null) {
            String note = discount.freeShipping().label();
            if (discount.freeShipping().detail() != null) {
                note += " — " + discount.freeShipping().detail();
            }
            order.setFreeShippingNote(note);
        }

        return repo.save(order);
    }

    /**
     * Venta cargada en el local (POS): crea el pedido, lo marca como canal LOCAL
     * y lo confirma en el acto (descuenta stock, queda PROCESADO). Si falta stock,
     * el confirm tira 400 y no se guarda nada (misma transacción).
     */
    public Order createPos(CreateOrderRequest req) {
        Order order = create(req);
        order.setChannel(com.estilospequenos.model.SaleChannel.LOCAL);
        order.setDeliveryMethod(DeliveryMethod.PICKUP);
        repo.save(order);
        return confirm(order.getId());
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

    /**
     * Confirma: descuenta stock de las líneas aceptadas y marca PROCESADO.
     * <b>Estricto</b>: si alguna línea aceptada no tiene stock suficiente, no
     * confirma nada y devuelve 400 con el detalle de lo que falta.
     */
    public Order confirm(String orderId) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue procesado o cancelado.");
        }

        // Pre-chequeo: no tocar stock hasta saber que alcanza para todo.
        List<String> shortages = new ArrayList<>();
        for (OrderLine l : order.getLines()) {
            if (!l.isAccepted()) continue;
            int available = productRepo.findById(l.getProductId())
                    .map(p -> p.getSizeStocks().stream()
                            .filter(s -> s.getSize().equals(l.getSize()))
                            .mapToInt(com.estilospequenos.model.SizeStock::getStock)
                            .findFirst().orElse(0))
                    .orElse(0);
            if (available < l.getQuantity()) {
                shortages.add(l.getProductName() + " (talle " + l.getSize() + "): pediste "
                        + l.getQuantity() + ", quedan " + available);
            }
        }
        if (!shortages.isEmpty()) {
            throw new BadRequestException("No hay stock suficiente para confirmar. "
                    + "Ajustá el stock o destildá estos ítems: " + String.join("; ", shortages) + ".");
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

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static Map<String, List<String>> paramsOf(Product p) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (ProductParam pp : p.getParams()) {
            map.computeIfAbsent(pp.getGroupId(), k -> new ArrayList<>()).add(pp.getOptionId());
        }
        return map;
    }
}
