package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    /** Correlativo. `code` se deriva de acá: "PED-" + %04d. */
    @Column(nullable = false, unique = true)
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
