package com.estilospequenos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cambio de prenda registrado en el local: el cliente devuelve una o más
 * prendas y se lleva otras. El stock de las devueltas vuelve a sumar y el de
 * las que se lleva se descuenta. Si lo que se lleva vale más, se cobra la
 * diferencia ({@code difference} positivo); si vale menos, queda a favor del
 * cliente ({@code difference} negativo).
 */
@Entity
@Table(name = "exchange")
public class Exchange {

    @Id
    private String id;

    /** Correlativo → code "CAM-0001". */
    @Column(nullable = false, unique = true)
    private long number;

    @Column(nullable = false)
    private String customerName;

    /** Total de lo devuelto (a precio de lista actual). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal returnedTotal = BigDecimal.ZERO;

    /** Total de lo que se lleva. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal takenTotal = BigDecimal.ZERO;

    /** takenTotal - returnedTotal. Positivo = cobra el local; negativo = a favor del cliente. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    /** Medio de pago de la diferencia (solo si difference > 0). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** DNI de quién lo procesó (null en cambios viejos). */
    @Column(length = 20)
    private String processedByDni;
    /** Snapshot del nombre. */
    @Column(length = 200)
    private String processedByName;

    @OneToMany(mappedBy = "exchange", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "idx")
    private List<ExchangeLine> lines = new ArrayList<>();

    @Transient
    public String getCode() {
        return "CAM-" + String.format("%04d", number);
    }

    public void addLine(ExchangeLine line) {
        line.setExchange(this);
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

    public BigDecimal getReturnedTotal() {
        return returnedTotal;
    }

    public void setReturnedTotal(BigDecimal returnedTotal) {
        this.returnedTotal = returnedTotal;
    }

    public BigDecimal getTakenTotal() {
        return takenTotal;
    }

    public void setTakenTotal(BigDecimal takenTotal) {
        this.takenTotal = takenTotal;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExchangeLine> getLines() {
        return lines;
    }

    public void setLines(List<ExchangeLine> lines) {
        this.lines = lines;
    }

    public String getProcessedByDni() {
        return processedByDni;
    }

    public void setProcessedByDni(String processedByDni) {
        this.processedByDni = processedByDni;
    }

    public String getProcessedByName() {
        return processedByName;
    }

    public void setProcessedByName(String processedByName) {
        this.processedByName = processedByName;
    }
}
