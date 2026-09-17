package com.saasweb.core.order;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "number"}))
public class Order {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Correlativo (por tenant). `code` se deriva de acá: "PED-" + %04d. */
    @Column(nullable = false)
    private long number;

    @Column(nullable = false)
    private String customerName;

    /** Opcional: para armar la base de clientes y mandar campañas de marketing. */
    @Column(length = 200)
    private String customerEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private int discountPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDIENTE;

    /** De dónde vino: WEB (checkout) o LOCAL (venta cargada a mano en el panel). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SaleChannel channel = SaleChannel.WEB;

    /** Entrega elegida por el cliente. Los pedidos viejos quedan en PICKUP. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryMethod deliveryMethod = DeliveryMethod.PICKUP;

    /** Dirección de envío normalizada (solo si deliveryMethod = SHIPPING). */
    @Column(length = 500)
    private String shippingAddress;

    /** Aclaración/referencia de la dirección (piso, depto, entre calles…). */
    @Column(length = 500)
    private String shippingReference;

    /** Coordenadas del pin confirmado en el mapa (para abrir en Google Maps). */
    private Double shippingLat;
    private Double shippingLng;

    /** Medio de pago elegido por el cliente. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Venta presencial en efectivo: con cuánto pagó el cliente — para calcular
     * el vuelto en el momento y dejarlo anotado. Sólo tiene sentido con
     * {@code paymentMethod = CASH}; null en cualquier otro caso o si no se
     * cargó (es opcional incluso pagando en efectivo).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal amountTendered;

    /**
     * Referencia anotada a mano del pago presencial — según el medio significa
     * una cosa distinta: con {@code TRANSFER}, nombre y apellido de quien
     * transfirió (para poder cruzarlo con el resumen bancario); con
     * {@code POSNET}, el número de ticket que imprime la máquina al aprobar.
     * No hay integración real con el posnet ni con el banco, es sólo una nota
     * para poder reconciliar después. Opcional siempre.
     */
    @Column(length = 200)
    private String paymentReference;

    /**
     * Comprobante emitido para esta venta presencial (Fase 14, ver
     * PLAN_SAAS.md) — sólo tiene sentido para pedidos armados desde el
     * punto de venta (kiosco). {@code null} = todavía no se emitió nada
     * (pedidos viejos, o de canal WEB). "TICKET_INTERNO" = comprobante no
     * fiscal; "FACTURA_C"/"FACTURA_B"/"FACTURA_A" = factura real con CAE
     * de ARCA (el tipo lo decide `ArcaInvoiceService` según la condición
     * frente al IVA del tenant y si se cargó CUIT del comprador).
     */
    @Column(length = 20)
    private String invoiceType;

    /**
     * CUIT del comprador, sólo si se cargó al cobrar (habilita Factura A
     * en vez de B para un tenant Responsable Inscripto). Opcional siempre
     * — sin esto, Factura A/B/C se emite igual a consumidor final/DNI.
     */
    @Column(length = 20)
    private String invoiceBuyerCuit;

    /** CAE (Código de Autorización Electrónico) que devolvió ARCA — sólo con invoiceType = FACTURA_A/B/C aprobada. */
    @Column(length = 20)
    private String invoiceCae;

    /** Vencimiento del CAE, formato yyyyMMdd (tal cual lo devuelve ARCA). */
    @Column(length = 10)
    private String invoiceCaeVencimiento;

    /** Número de comprobante asignado (correlativo del punto de venta en ARCA, no el `number`/código interno del pedido). */
    private Long invoiceNumber;

    private Integer invoicePuntoVenta;

    /** Link al QR que exige ARCA en todo comprobante (RG 4892) — se arma una sola vez, al aprobarse el CAE. */
    @Column(length = 500)
    private String invoiceQrUrl;

    /**
     * Si se intentó pedir una Factura C y ARCA la rechazó (o falló la
     * conexión), el detalle queda acá para poder reintentar — la VENTA en
     * sí no se bloquea por esto: el cliente ya pagó, plantarse esperando a
     * ARCA no tiene sentido (ver `ArcaInvoiceService`).
     */
    @Column(length = 500)
    private String invoiceError;

    /** Sólo para `paymentMethod = MERCADOPAGO` — null para cualquier otro medio. Ver {@link PaymentStatus}. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus paymentStatus;

    /** Id de la preferencia de pago creada en Mercado Pago (ver `core/payment/MercadoPagoService`). */
    @Column(length = 100)
    private String mpPreferenceId;

    /** Link de pago (`init_point`) devuelto al crear la preferencia — para poder reofrecerlo si el cliente no pagó todavía. */
    @Column(length = 500)
    private String mpCheckoutUrl;

    /** Id del pago aprobado en Mercado Pago, una vez confirmado por webhook. */
    @Column(length = 100)
    private String mpPaymentId;

    /**
     * Mercado Pago aprobó el pago pero {@code OrderService#doConfirm} no
     * pudo confirmar el pedido solo (ej. se quedó sin stock justo antes de
     * que se apruebe) — el pedido queda igual en PENDIENTE, sin tocar
     * stock, pero esto queda como aviso visible en el panel para que
     * alguien lo revise a mano (antes esto quedaba sólo en un log del
     * servidor, invisible para la tienda). {@code null} = sin problemas.
     */
    @Column(length = 500)
    private String paymentIssueNote;

    /** Si al crear el pedido aplicaba "envío gratis": el texto para mostrarle al cliente. null = no. */
    @Column(length = 300)
    private String freeShippingNote;

    /** Letra chica de los descuentos aplicados (ej: "solo microcentro"), unida con "; ". */
    @Column(length = 500)
    private String discountNote;

    /** Código de cupón aplicado (null = ninguno). */
    @Column(length = 40)
    private String couponCode;

    /** Descuento en pesos del cupón (aparte del descuento automático). */
    @Column(precision = 12, scale = 2)
    private BigDecimal couponDiscount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant processedAt;

    /** DNI de quién armó el pedido (null en el checkout web público). */
    @Column(length = 20)
    private String createdByDni;
    /** Snapshot del nombre — no se rompe si el usuario cambia de nombre o se borra después. */
    @Column(length = 200)
    private String createdByName;

    /** DNI de quién lo confirmó/cobró (se completa recién al confirmar). */
    @Column(length = 20)
    private String confirmedByDni;
    @Column(length = 200)
    private String confirmedByName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "idx")
    private List<OrderLine> lines = new ArrayList<>();

    @Transient
    public String getCode() {
        return "PED-" + String.format("%04d", number);
    }

    public void addLine(OrderLine line) {
        line.setOrder(this);
        lines.add(line);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public SaleChannel getChannel() {
        return channel;
    }

    public void setChannel(SaleChannel channel) {
        this.channel = channel;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingReference() {
        return shippingReference;
    }

    public void setShippingReference(String shippingReference) {
        this.shippingReference = shippingReference;
    }

    public Double getShippingLat() {
        return shippingLat;
    }

    public void setShippingLat(Double shippingLat) {
        this.shippingLat = shippingLat;
    }

    public Double getShippingLng() {
        return shippingLng;
    }

    public void setShippingLng(Double shippingLng) {
        this.shippingLng = shippingLng;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmountTendered() {
        return amountTendered;
    }

    public void setAmountTendered(BigDecimal amountTendered) {
        this.amountTendered = amountTendered;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoiceBuyerCuit() {
        return invoiceBuyerCuit;
    }

    public void setInvoiceBuyerCuit(String invoiceBuyerCuit) {
        this.invoiceBuyerCuit = invoiceBuyerCuit;
    }

    public String getInvoiceCae() {
        return invoiceCae;
    }

    public void setInvoiceCae(String invoiceCae) {
        this.invoiceCae = invoiceCae;
    }

    public String getInvoiceCaeVencimiento() {
        return invoiceCaeVencimiento;
    }

    public void setInvoiceCaeVencimiento(String invoiceCaeVencimiento) {
        this.invoiceCaeVencimiento = invoiceCaeVencimiento;
    }

    public Long getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(Long invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Integer getInvoicePuntoVenta() {
        return invoicePuntoVenta;
    }

    public void setInvoicePuntoVenta(Integer invoicePuntoVenta) {
        this.invoicePuntoVenta = invoicePuntoVenta;
    }

    public String getInvoiceQrUrl() {
        return invoiceQrUrl;
    }

    public void setInvoiceQrUrl(String invoiceQrUrl) {
        this.invoiceQrUrl = invoiceQrUrl;
    }

    public String getInvoiceError() {
        return invoiceError;
    }

    public void setInvoiceError(String invoiceError) {
        this.invoiceError = invoiceError;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getMpPreferenceId() {
        return mpPreferenceId;
    }

    public void setMpPreferenceId(String mpPreferenceId) {
        this.mpPreferenceId = mpPreferenceId;
    }

    public String getMpCheckoutUrl() {
        return mpCheckoutUrl;
    }

    public void setMpCheckoutUrl(String mpCheckoutUrl) {
        this.mpCheckoutUrl = mpCheckoutUrl;
    }

    public String getMpPaymentId() {
        return mpPaymentId;
    }

    public void setMpPaymentId(String mpPaymentId) {
        this.mpPaymentId = mpPaymentId;
    }

    public String getPaymentIssueNote() {
        return paymentIssueNote;
    }

    public void setPaymentIssueNote(String paymentIssueNote) {
        this.paymentIssueNote = paymentIssueNote;
    }

    public String getFreeShippingNote() {
        return freeShippingNote;
    }

    public void setFreeShippingNote(String freeShippingNote) {
        this.freeShippingNote = freeShippingNote;
    }

    public String getDiscountNote() {
        return discountNote;
    }

    public void setDiscountNote(String discountNote) {
        this.discountNote = discountNote;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(BigDecimal couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
    }

    public String getCreatedByDni() {
        return createdByDni;
    }

    public void setCreatedByDni(String createdByDni) {
        this.createdByDni = createdByDni;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getConfirmedByDni() {
        return confirmedByDni;
    }

    public void setConfirmedByDni(String confirmedByDni) {
        this.confirmedByDni = confirmedByDni;
    }

    public String getConfirmedByName() {
        return confirmedByName;
    }

    public void setConfirmedByName(String confirmedByName) {
        this.confirmedByName = confirmedByName;
    }
}
