package com.estilospequenos.service;

import com.estilospequenos.common.BadRequestException;
import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.DiscountDtos.CartDiscountResult;
import com.estilospequenos.dto.OrderDtos.CartItem;
import com.estilospequenos.dto.OrderDtos.CreateOrderRequest;
import com.estilospequenos.config.AppProperties;
import com.estilospequenos.model.DeliveryMethod;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.PaymentMethod;
import com.estilospequenos.model.PaymentStatus;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.ProductParam;
import com.estilospequenos.repository.AdminUserRepository;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ProductRepository;
import com.estilospequenos.service.DiscountService.CartLineInput;
import com.estilospequenos.service.DiscountService;
import com.estilospequenos.service.MercadoPagoService.PreferenceItem;
import com.estilospequenos.service.MercadoPagoService.PreferenceResult;
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
    private final AdminUserRepository adminUsers;
    private final SiteSettingsService siteSettingsService;
    private final MercadoPagoService mercadoPagoService;
    private final AppProperties appProperties;
    private final OrderMailService orderMailService;

    public OrderService(OrderRepository repo, ProductRepository productRepo,
                        ProductService productService, DiscountService discountService,
                        CouponService couponService, AdminUserRepository adminUsers,
                        SiteSettingsService siteSettingsService, MercadoPagoService mercadoPagoService,
                        AppProperties appProperties, OrderMailService orderMailService) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.productService = productService;
        this.discountService = discountService;
        this.couponService = couponService;
        this.adminUsers = adminUsers;
        this.siteSettingsService = siteSettingsService;
        this.mercadoPagoService = mercadoPagoService;
        this.appProperties = appProperties;
        this.orderMailService = orderMailService;
    }

    /** Resuelve el nombre a mostrar de un usuario del panel a partir de su DNI. */
    private String nameByDni(String dni) {
        if (dni == null || dni.isBlank()) return null;
        return adminUsers.findByDni(dni).map(u -> u.getNombre() + " " + u.getApellido()).orElse(null);
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
        order.setCustomerEmail(normalizeEmail(req.customerEmail()));
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
            line.setStatus(com.estilospequenos.model.OrderLineStatus.PENDIENTE);
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
     * Crea el pedido desde el checkout público (carrito online) — a
     * diferencia de {@link #create}, si el medio de pago es Mercado Pago
     * arranca el checkout online de verdad (crea la preferencia). NO la usa
     * {@link #createPos}: una venta armada en el local con
     * `paymentMethod = MERCADOPAGO` es sólo una etiqueta ("me pagaron por
     * Mercado Pago en el momento") — no tiene que generar ningún link de
     * pago ni redirigir a nadie.
     * <p>{@code noRollbackFor}: si falla la preferencia de MP (token
     * inválido, red caída), el pedido YA creado no se pierde — la clase es
     * {@code @Transactional} y sin esto, el rollback automático por
     * {@link BadRequestException} se llevaba puesto el insert del pedido
     * también (probado a mano: quedaba 403 de MP y el pedido no aparecía en
     * el panel).</p>
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public Order createWebCheckout(CreateOrderRequest req) {
        // Si la tienda activó Mercado Pago, pasa a ser el ÚNICO medio de pago
        // online (no se puede elegir transferencia/QR a la vez) — ver
        // PROYECTO.md. Si no lo activó, tampoco se puede elegir (todavía no
        // hay token cargado).
        boolean mpEnabled = siteSettingsService.get().isMpEnabled();
        if (mpEnabled && req.paymentMethod() != PaymentMethod.MERCADOPAGO) {
            throw new BadRequestException("Esta tienda solo acepta Mercado Pago como medio de pago online.");
        }
        if (!mpEnabled && req.paymentMethod() == PaymentMethod.MERCADOPAGO) {
            throw new BadRequestException("Esta tienda todavía no configuró Mercado Pago.");
        }
        // Un pedido pagado por Mercado Pago no deja ningún registro fuera del
        // sitio (a diferencia de los coordinados por WhatsApp, que le quedan
        // al cliente en su propio chat) — sin mail no hay forma de mandarle
        // el comprobante ni que pueda reclamar algo después.
        if (req.paymentMethod() == PaymentMethod.MERCADOPAGO
                && (req.customerEmail() == null || req.customerEmail().isBlank())) {
            throw new BadRequestException("Para pagar con Mercado Pago necesitamos tu mail (te mandamos el comprobante ahí).");
        }
        Order order = create(req);
        if (order.getPaymentMethod() == PaymentMethod.MERCADOPAGO) {
            startMercadoPagoCheckout(order);
        }
        return order;
    }

    /**
     * Crea la preferencia de pago en Mercado Pago para este pedido y guarda
     * el link de checkout — se llama recién con el pedido ya guardado (hace
     * falta el id para `external_reference`/`back_urls`). Si algo falla acá
     * (token inválido, red caída), no se pierde el pedido: queda creado
     * igual, `paymentStatus = PENDING` sin `mpCheckoutUrl`, y el error sube
     * al caller para que el checkout le avise al cliente que reintente.
     */
    private void startMercadoPagoCheckout(Order order) {
        var settings = siteSettingsService.get();
        String accessToken = settings.getMpAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new BadRequestException("Esta tienda todavía no configuró Mercado Pago.");
        }

        List<PreferenceItem> items = order.getLines().stream()
                .map(l -> new PreferenceItem(l.getProductName(), l.getQuantity(), l.getUnitPrice()))
                .toList();

        String backend = appProperties.getUrls().getBackend();
        String frontend = appProperties.getUrls().getFrontend();
        String notificationUrl = backend + "/api/webhooks/mercadopago";
        String returnBase = frontend + "/mis-pedidos?code=" + order.getCode();

        PreferenceResult pref = mercadoPagoService.createPreference(
                accessToken, order.getId(), items, notificationUrl,
                returnBase + "&pago=aprobado", returnBase + "&pago=pendiente", returnBase + "&pago=rechazado");

        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setMpPreferenceId(pref.preferenceId());
        order.setMpCheckoutUrl(pref.initPoint());
        repo.save(order);
    }

    /**
     * Venta armada en el local (POS): crea el pedido como canal LOCAL, queda
     * PENDIENTE ("armado, pendiente de cobro"). Registra quién lo armó.
     * El cobro es un paso aparte ({@link #confirm}) — puede hacerlo la misma
     * persona (encadenado desde el frontend) u otra (cajero).
     */
    public Order createPos(CreateOrderRequest req, String createdByDni) {
        // La venta en el local no usa Mercado Pago (efectivo/transferencia/
        // posnet nomás) — Checkout Pro es solo para el carrito online.
        if (req.paymentMethod() == PaymentMethod.MERCADOPAGO) {
            throw new BadRequestException("Mercado Pago no es un medio de pago válido para la venta en el local.");
        }
        Order order = create(req);
        order.setChannel(com.estilospequenos.model.SaleChannel.LOCAL);
        order.setDeliveryMethod(DeliveryMethod.PICKUP);
        order.setCreatedByDni(createdByDni);
        order.setCreatedByName(nameByDni(createdByDni));
        return repo.save(order);
    }

    /**
     * Confirma TODO lo que sigue pendiente del pedido — atajo sobre
     * {@link #confirmLines} para cuando no hace falta entrega parcial.
     * Registra quién cobró/confirmó (puede ser distinto de quién armó el
     * pedido).
     */
    public Order confirm(String orderId, String confirmedByDni) {
        Order order = get(orderId);
        return confirmLinesInternal(order, pendingLineIds(order), confirmedByDni);
    }

    /**
     * Confirma automáticamente un pedido cuyo pago online fue aprobado (ver
     * MercadoPagoWebhookController) — mismo descuento de stock que
     * {@link #confirm}, sin DNI de un humano (queda registrado como
     * "Mercado Pago" en vez de un nombre de usuario del panel).
     */
    public Order confirmFromPayment(String orderId, String mpPaymentId) {
        Order order = get(orderId);
        order.setMpPaymentId(mpPaymentId);
        order.setPaymentStatus(PaymentStatus.APPROVED);
        Order confirmed = confirmLinesInternal(order, pendingLineIds(order), null);
        orderMailService.sendOrderConfirmation(confirmed);
        return confirmed;
    }

    /**
     * Entrega parcial: confirma sólo las líneas indicadas (descuenta su
     * stock, congela su costo) y deja el resto tal cual. Si con esto no
     * queda ninguna línea PENDIENTE, el pedido pasa a PROCESADO (si algo se
     * entregó) o CANCELADO (si todo terminó cancelado). <b>Estricto</b>: si
     * alguna de las líneas pedidas no tiene stock suficiente, no confirma
     * ninguna del grupo y devuelve 400 con el detalle.
     */
    public Order confirmLines(String orderId, List<String> lineIds, String confirmedByDni) {
        return confirmLinesInternal(get(orderId), lineIds, confirmedByDni);
    }

    /** Cancela sólo las líneas indicadas (no tocan stock, nunca lo tocaron). */
    public Order cancelLines(String orderId, List<String> lineIds) {
        return cancelLinesInternal(get(orderId), lineIds);
    }

    /**
     * El pago online fue rechazado/cancelado — el pedido nunca llegó a
     * tocar stock (recién se descuenta al confirmar), así que cancelarlo es
     * seguro. No hace nada si ya estaba en otro estado (ej. el cliente pagó
     * en un segundo intento y ya se confirmó antes de que llegue esta
     * notificación vieja).
     */
    public void markPaymentRejected(String orderId) {
        Order order = get(orderId);
        order.setPaymentStatus(PaymentStatus.REJECTED);
        if (order.getStatus() == OrderStatus.PENDIENTE) {
            order.setStatus(OrderStatus.CANCELADO);
            order.setProcessedAt(Instant.now());
        }
        repo.save(order);
    }

    /** Cancela TODO lo que sigue pendiente — atajo sobre {@link #cancelLines}. */
    public Order cancel(String orderId) {
        Order order = get(orderId);
        return cancelLinesInternal(order, pendingLineIds(order));
    }

    private static List<String> pendingLineIds(Order order) {
        return order.getLines().stream()
                .filter(l -> l.getStatus() == com.estilospequenos.model.OrderLineStatus.PENDIENTE)
                .map(OrderLine::getId)
                .toList();
    }

    private static void requirePending(Order order) {
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue procesado o cancelado.");
        }
    }

    /** Busca entre las líneas del pedido las que pidieron por id, validando que existan y sigan pendientes. */
    private static List<OrderLine> pendingTargets(Order order, List<String> lineIds) {
        List<OrderLine> targets = order.getLines().stream()
                .filter(l -> lineIds.contains(l.getId()))
                .toList();
        if (targets.isEmpty()) {
            throw new BadRequestException("No se encontraron las líneas indicadas.");
        }
        for (OrderLine l : targets) {
            if (l.getStatus() != com.estilospequenos.model.OrderLineStatus.PENDIENTE) {
                throw new BadRequestException(l.getProductName() + " (talle " + l.getSize()
                        + ") ya fue " + l.getStatus().name().toLowerCase() + ".");
            }
        }
        return targets;
    }

    private Order confirmLinesInternal(Order order, List<String> lineIds, String confirmedByDni) {
        requirePending(order);
        List<OrderLine> targets = pendingTargets(order, lineIds);

        // Pre-chequeo: no tocar stock hasta saber que alcanza para todo el grupo.
        List<String> shortages = new ArrayList<>();
        for (OrderLine l : targets) {
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
            throw new BadRequestException("No hay stock suficiente para entregar. " + String.join("; ", shortages) + ".");
        }

        for (OrderLine l : targets) {
            productService.decrementStock(l.getProductId(), l.getSize(), l.getQuantity(),
                    com.estilospequenos.model.StockMovementReason.VENTA, order.getId(), confirmedByDni);
            // Congela el costo ACÁ (no en create()): es el momento real en que
            // el producto sale de stock. Así la ganancia de esta línea no
            // cambia después si se actualiza el costo del producto (ver
            // OrderLine.costPrice).
            productRepo.findById(l.getProductId()).ifPresent(p -> l.setCostPrice(p.getCostPrice()));
            l.setStatus(com.estilospequenos.model.OrderLineStatus.ENTREGADA);
        }
        order.setConfirmedByDni(confirmedByDni);
        order.setConfirmedByName(nameByDni(confirmedByDni));
        recomputeOrderStatus(order);
        return repo.save(order);
    }

    private Order cancelLinesInternal(Order order, List<String> lineIds) {
        requirePending(order);
        List<OrderLine> targets = pendingTargets(order, lineIds);
        for (OrderLine l : targets) {
            l.setStatus(com.estilospequenos.model.OrderLineStatus.CANCELADA);
        }
        recomputeOrderStatus(order);
        return repo.save(order);
    }

    /**
     * El pedido resuelve recién cuando ninguna línea sigue PENDIENTE:
     * PROCESADO si algo se llegó a entregar, CANCELADO si todo terminó
     * cancelado. Mientras quede aunque sea una línea pendiente, el pedido
     * sigue PENDIENTE (entrega parcial en curso).
     */
    private void recomputeOrderStatus(Order order) {
        boolean anyPending = order.getLines().stream()
                .anyMatch(l -> l.getStatus() == com.estilospequenos.model.OrderLineStatus.PENDIENTE);
        if (anyPending) return;
        boolean anyDelivered = order.getLines().stream()
                .anyMatch(l -> l.getStatus() == com.estilospequenos.model.OrderLineStatus.ENTREGADA);
        order.setStatus(anyDelivered ? OrderStatus.PROCESADO : OrderStatus.CANCELADO);
        order.setProcessedAt(Instant.now());
    }

    /**
     * Agrega un ítem a un pedido pendiente (ej: el cliente llamó a sumar
     * algo antes de que se lo lleven). Sólo mientras el pedido sigue
     * PENDIENTE — una vez que hay líneas entregadas/canceladas, para tocar
     * ese pedido hay que ir por Cambios.
     */
    public Order addLine(String orderId, String productId, String size, int quantity) {
        Order order = get(orderId);
        requirePending(order);
        if (quantity <= 0) throw new BadRequestException("La cantidad tiene que ser mayor a 0.");
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productId));
        OrderLine line = new OrderLine();
        line.setId(UUID.randomUUID().toString());
        line.setProductId(p.getId());
        line.setProductName(p.getName());
        line.setSize(size);
        line.setQuantity(quantity);
        line.setUnitPrice(p.getPrice());
        line.setStatus(com.estilospequenos.model.OrderLineStatus.PENDIENTE);
        order.addLine(line);
        recomputeTotals(order);
        return repo.save(order);
    }

    /** Cambia la cantidad de una línea todavía pendiente del pedido. */
    public Order updateLineQuantity(String orderId, String lineId, int quantity) {
        Order order = get(orderId);
        requirePending(order);
        if (quantity <= 0) throw new BadRequestException("La cantidad tiene que ser mayor a 0.");
        OrderLine line = order.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Línea de pedido", lineId));
        if (line.getStatus() != com.estilospequenos.model.OrderLineStatus.PENDIENTE) {
            throw new BadRequestException("Esa línea ya fue " + line.getStatus().name().toLowerCase() + ".");
        }
        line.setQuantity(quantity);
        recomputeTotals(order);
        return repo.save(order);
    }

    /** Saca una línea todavía pendiente del pedido (no una entregada/cancelada: para eso está Cambios). */
    public Order removeLine(String orderId, String lineId) {
        Order order = get(orderId);
        requirePending(order);
        OrderLine line = order.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Línea de pedido", lineId));
        if (order.getLines().size() <= 1) {
            throw new BadRequestException("El pedido tiene que tener al menos un ítem — cancelalo si no va ninguno.");
        }
        order.getLines().remove(line);
        recomputeTotals(order);
        return repo.save(order);
    }

    /**
     * Recalcula subtotal/total tras editar líneas de un pedido pendiente. A
     * propósito NO vuelve a correr el motor de descuentos (tiers por monto,
     * cupón, envío gratis, etc): esos se calcularon una única vez en
     * {@link #create} contra el carrito original y quedan fijos — recalcularlos
     * en cada edición podría cambiarle el % de descuento al cliente sin que lo
     * sepa. El descuento/cupón ya aplicado se resta tal cual quedó.
     */
    private void recomputeTotals(Order order) {
        BigDecimal subtotal = order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal coupon = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
        order.setTotal(subtotal.subtract(discount).subtract(coupon).max(BigDecimal.ZERO));
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** Normaliza a minúsculas/sin espacios para que las queries de segmentación de marketing no necesiten LOWER(). */
    private static String normalizeEmail(String v) {
        String trimmed = blankToNull(v);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private static Map<String, List<String>> paramsOf(Product p) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (ProductParam pp : p.getParams()) {
            map.computeIfAbsent(pp.getGroupId(), k -> new ArrayList<>()).add(pp.getOptionId());
        }
        return map;
    }
}
