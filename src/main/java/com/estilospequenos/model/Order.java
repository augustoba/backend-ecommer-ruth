package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    /** Correlativo. `code` se deriva de acá: "PED-" + %04d. */
    @Column(nullable = false, unique = true)
    private long number;

    @Column(nullable = false)
    private String customerName;

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
}
