package com.saasweb.core.order;

import com.saasweb.common.BadRequestException;
import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.discount.DiscountDtos.CartDiscountResult;
import com.saasweb.core.order.OrderDtos.CartItem;
import com.saasweb.core.order.OrderDtos.CreateOrderRequest;
import com.saasweb.core.order.OrderDtos.LineAcceptance;
import com.saasweb.core.order.DeliveryMethod;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderLine;
import com.saasweb.core.order.OrderStatus;
import com.saasweb.core.product.Product;
import com.saasweb.core.product.ProductParam;
import com.saasweb.core.admin.AdminUserRepository;
import com.saasweb.core.order.OrderRepository;
import com.saasweb.core.product.ProductRepository;
import com.saasweb.core.discount.DiscountService.CartLineInput;
import com.saasweb.core.discount.DiscountService;
import com.saasweb.core.coupon.CouponService;
import com.saasweb.core.payment.MercadoPagoService;
import com.saasweb.core.payment.MercadoPagoService.PreferenceItem;
import com.saasweb.core.payment.MercadoPagoService.PreferenceResult;
import com.saasweb.core.arca.ArcaInvoiceService;
import com.saasweb.core.product.ProductService;
import com.saasweb.core.settings.SiteSettings;
import com.saasweb.core.settings.SiteSettingsService;
import com.saasweb.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
    private final ArcaInvoiceService arcaInvoiceService;
    private final com.saasweb.core.arca.InvoiceMailService invoiceMailService;

    public OrderService(OrderRepository repo, ProductRepository productRepo,
                        ProductService productService, DiscountService discountService,
                        CouponService couponService, AdminUserRepository adminUsers,
                        SiteSettingsService siteSettingsService, MercadoPagoService mercadoPagoService,
                        AppProperties appProperties, OrderMailService orderMailService,
                        ArcaInvoiceService arcaInvoiceService, com.saasweb.core.arca.InvoiceMailService invoiceMailService) {
        this.orderMailService = orderMailService;
        this.repo = repo;
        this.productRepo = productRepo;
        this.productService = productService;
        this.discountService = discountService;
        this.couponService = couponService;
        this.adminUsers = adminUsers;
        this.siteSettingsService = siteSettingsService;
        this.mercadoPagoService = mercadoPagoService;
        this.appProperties = appProperties;
        this.arcaInvoiceService = arcaInvoiceService;
        this.invoiceMailService = invoiceMailService;
    }

    /** Resuelve el nombre a mostrar de un usuario del panel a partir de su DNI. */
    private String nameByDni(String dni) {
        if (dni == null || dni.isBlank()) return null;
        return adminUsers.findByDniForTenant(dni, TenantContext.getTenantId())
                .map(u -> u.getNombre() + " " + u.getApellido()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return repo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> findAll(org.springframework.data.domain.Pageable pageable) {
        return repo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId(), pageable);
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
        return repo.search(TenantContext.getTenantId(), status, fromI, toI, s, like, num, pageable);
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", id));
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
        return repo.findByTenantIdAndNumber(TenantContext.getTenantId(), number)
                .filter(o -> o.getCustomerName().trim().equalsIgnoreCase(name.trim()))
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", code));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repo.countByTenantIdAndStatus(TenantContext.getTenantId(), OrderStatus.PENDIENTE);
    }

    /** Crea el pedido desde el carrito: totales + descuentos calculados server-side. */
    public Order create(CreateOrderRequest req) {
        String tenantId = TenantContext.getTenantId();
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setTenantId(tenantId);
        order.setNumber(repo.maxNumber(tenantId) + 1);
        order.setCustomerName(
                req.customerName() == null || req.customerName().isBlank()
                        ? "Sin nombre" : req.customerName().trim());
        order.setCustomerEmail(normalizeEmail(req.customerEmail()));
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.PENDIENTE);

        DeliveryMethod delivery = req.deliveryMethod() != null ? req.deliveryMethod() : DeliveryMethod.PICKUP;
        order.setDeliveryMethod(delivery);
        order.setPaymentMethod(req.paymentMethod());
        order.setPaymentReference(blankToNull(req.paymentReference()));
        order.setInvoiceBuyerCuit(blankToNull(req.buyerCuit()));
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
            Product p = productRepo.findByIdAndTenantId(item.productId(), tenantId)
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

        if (req.amountTendered() != null) {
            if (order.getPaymentMethod() != PaymentMethod.CASH) {
                throw new BadRequestException("El vuelto sólo tiene sentido pagando en efectivo.");
            }
            if (req.amountTendered().compareTo(order.getTotal()) < 0) {
                throw new BadRequestException("Lo que puso el cliente no alcanza para cubrir el total.");
            }
            order.setAmountTendered(req.amountTendered());
        }

        String notes = discount.breakdown().stream()
                .map(com.saasweb.core.discount.DiscountDtos.BreakdownLine::detail)
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
     */
    public Order createWebCheckout(CreateOrderRequest req) {
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
        String notificationUrl = backend + "/api/webhooks/mercadopago?tenantId=" + order.getTenantId();
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
        Order order = create(req);
        order.setChannel(com.saasweb.core.order.SaleChannel.LOCAL);
        order.setDeliveryMethod(DeliveryMethod.PICKUP);
        order.setCreatedByDni(createdByDni);
        order.setCreatedByName(nameByDni(createdByDni));
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

    /**
     * Confirma: descuenta stock de las líneas aceptadas y marca PROCESADO.
     * <b>Estricto</b>: si alguna línea aceptada no tiene stock suficiente, no
     * confirma nada y devuelve 400 con el detalle de lo que falta. Registra
     * quién cobró/confirmó (puede ser distinto de quién armó el pedido).
     */
    public Order confirm(String orderId, String confirmedByDni) {
        Order order = get(orderId);
        return doConfirm(order, confirmedByDni, nameByDni(confirmedByDni));
    }

    /**
     * Confirma automáticamente un pedido cuyo pago online fue aprobado (ver
     * `MercadoPagoWebhookController`) — mismo descuento de stock que
     * {@link #confirm}, sin DNI de un humano (queda registrado como
     * "Mercado Pago" en vez de un nombre de usuario del panel).
     *
     * <p>Si {@code doConfirm} falla (ej. se quedó sin stock justo antes de
     * que se apruebe el pago), el pedido queda igual en PENDIENTE sin tocar
     * stock — pero a diferencia de antes, el pago SÍ queda marcado como
     * aprobado (el cliente pagó de verdad, eso no hay que perderlo) y
     * {@code paymentIssueNote} deja un aviso visible en el panel para que
     * alguien lo revise a mano. Antes esto sólo quedaba en un log del
     * servidor — nadie del lado de la tienda se enteraba de que un cliente
     * había pagado y su pedido necesitaba atención manual.</p>
     */
    public Order confirmFromPayment(String orderId, String mpPaymentId) {
        Order order = get(orderId);
        order.setMpPaymentId(mpPaymentId);
        order.setPaymentStatus(PaymentStatus.APPROVED);
        try {
            Order confirmed = doConfirm(order, null, "Mercado Pago (pago validado)");
            orderMailService.sendOrderConfirmation(confirmed);
            return confirmed;
        } catch (RuntimeException e) {
            order.setPaymentIssueNote(
                    "Mercado Pago aprobó el pago pero no se pudo confirmar el pedido solo: "
                            + e.getMessage() + ". Revisalo y confirmalo a mano.");
            return repo.save(order);
        }
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

    /**
     * Confirma: descuenta stock de las líneas aceptadas y marca PROCESADO.
     * <b>Estricto</b>: si alguna línea aceptada no tiene stock suficiente, no
     * confirma nada y devuelve 400 con el detalle de lo que falta.
     */
    private Order doConfirm(Order order, String confirmedByDni, String confirmedByName) {
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new BadRequestException("El pedido ya fue procesado o cancelado.");
        }

        // Pre-chequeo: no tocar stock hasta saber que alcanza para todo.
        List<String> shortages = new ArrayList<>();
        for (OrderLine l : order.getLines()) {
            if (!l.isAccepted()) continue;
            int available = productRepo.findByIdAndTenantId(l.getProductId(), order.getTenantId())
                    .map(p -> p.getSizeStocks().stream()
                            .filter(s -> s.getSize().equals(l.getSize()))
                            .mapToInt(com.saasweb.modules.ropa.SizeStock::getStock)
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
                productService.decrementStock(l.getProductId(), l.getSize(), l.getQuantity(),
                        com.saasweb.core.product.StockMovementReason.VENTA, order.getId(), confirmedByDni);
                // Congela el costo ACÁ (no en create()): es el momento real en que
                // el producto sale de stock. Así la ganancia de esta línea no
                // cambia después si se actualiza el costo del producto (ver
                // OrderLine.costPrice).
                productRepo.findByIdAndTenantId(l.getProductId(), order.getTenantId())
                        .ifPresent(p -> l.setCostPrice(p.getCostPrice()));
            }
        }
        order.setStatus(OrderStatus.PROCESADO);
        order.setProcessedAt(Instant.now());
        order.setConfirmedByDni(confirmedByDni);
        order.setConfirmedByName(confirmedByName);
        if (order.getChannel() == SaleChannel.LOCAL) {
            applyInvoicing(order);
        }
        return repo.save(order);
    }

    /**
     * Venta presencial (kiosco / Venta en el local): emite el comprobante
     * según lo que el tenant tenga configurado. Si es "Factura ARCA" y ARCA
     * la rechaza (o falla la conexión), el pedido NO se cae — ya se
     * confirmó y descontó stock, y el cliente ya pagó; queda marcado como
     * ticket interno con el error anotado para reintentar a mano (ver
     * `Order.invoiceError`, `ArcaInvoiceService`).
     */
    private void applyInvoicing(Order order) {
        SiteSettings settings = siteSettingsService.get();
        if (!"FACTURA_ARCA".equals(settings.getInvoiceMode()) || !arcaInvoiceService.isAvailable()) {
            order.setInvoiceType("TICKET_INTERNO");
            return;
        }
        var result = arcaInvoiceService.emitirFactura(
                order.getTenantId(), invoiceLinesFor(order), null, order.getInvoiceBuyerCuit());
        if (result.aprobado()) {
            order.setInvoiceType(result.tipo());
            order.setInvoiceCae(result.cae());
            order.setInvoiceCaeVencimiento(result.caeVencimiento());
            order.setInvoiceNumber(result.numero());
            order.setInvoicePuntoVenta(result.puntoVenta());
            order.setInvoiceQrUrl(result.qrUrl());
            order.setInvoiceError(null);
        } else {
            order.setInvoiceType("TICKET_INTERNO");
            order.setInvoiceError(result.error());
        }
        try {
            invoiceMailService.send(order);
        } catch (Exception e) {
            log.warn("No se pudo mandar el comprobante por mail del pedido '{}': {}", order.getCode(), e.getMessage());
        }
    }

    /**
     * Arma las líneas que ve ARCA a partir de las líneas del pedido (importe
     * + alícuota de IVA real del producto, ver {@code Product.ivaRate}).
     * Si el pedido tiene un descuento aplicado, cada importe se escala
     * proporcionalmente para que la suma dé exactamente {@code order.getTotal()}
     * (lo que ARCA tiene que ver facturado), preservando el peso relativo de
     * cada alícuota — simplificación deliberada: no hay forma de saber en
     * qué línea "cayó" el descuento, así que se reparte proporcional a todas.
     */
    private java.util.List<com.saasweb.core.arca.ArcaInvoiceService.InvoiceLine> invoiceLinesFor(Order order) {
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        java.util.List<java.math.BigDecimal[]> raw = new java.util.ArrayList<>(); // [amount, ivaRate]
        for (OrderLine l : order.getLines()) {
            if (!l.isAccepted()) continue;
            java.math.BigDecimal amount = l.getUnitPrice().multiply(java.math.BigDecimal.valueOf(l.getQuantity()));
            subtotal = subtotal.add(amount);
            java.math.BigDecimal ivaRate = productRepo.findByIdAndTenantId(l.getProductId(), order.getTenantId())
                    .map(Product::getIvaRate).orElse(null);
            raw.add(new java.math.BigDecimal[]{amount, ivaRate});
        }
        java.math.BigDecimal total = order.getTotal();
        java.math.BigDecimal ratio = subtotal.signum() == 0 ? java.math.BigDecimal.ONE
                : total.divide(subtotal, 6, java.math.RoundingMode.HALF_UP);

        java.util.List<com.saasweb.core.arca.ArcaInvoiceService.InvoiceLine> lines = new java.util.ArrayList<>();
        for (java.math.BigDecimal[] r : raw) {
            java.math.BigDecimal scaled = r[0].multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP);
            lines.add(new com.saasweb.core.arca.ArcaInvoiceService.InvoiceLine(scaled, r[1]));
        }
        return lines;
    }

    /**
     * Reintenta emitir la factura de ARCA de una venta presencial que quedó
     * como ticket interno (porque ARCA la rechazó, falló la conexión, o
     * porque en ese momento la tienda todavía no tenía ARCA configurado) —
     * sin volver a cobrar ni tocar stock, el pedido ya está PROCESADO.
     */
    public Order retryInvoicing(String orderId) {
        Order order = get(orderId);
        if (order.getChannel() != SaleChannel.LOCAL || order.getStatus() != OrderStatus.PROCESADO) {
            throw new BadRequestException("Sólo se puede facturar una venta presencial ya cobrada.");
        }
        if (order.getInvoiceType() != null && order.getInvoiceType().startsWith("FACTURA_")) {
            throw new BadRequestException("Esta venta ya tiene una factura de ARCA aprobada.");
        }
        if (!arcaInvoiceService.isAvailable()) {
            throw new BadRequestException("Esta tienda todavía no configuró la facturación con ARCA.");
        }
        if (!"FACTURA_ARCA".equals(siteSettingsService.get().getInvoiceMode())) {
            throw new BadRequestException("Activá \"Factura C real, con CAE de ARCA\" en Configuración > Facturación (ARCA) para poder facturar.");
        }
        applyInvoicing(order);
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
