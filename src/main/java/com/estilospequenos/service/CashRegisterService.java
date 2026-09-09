package com.estilospequenos.service;

import com.estilospequenos.dto.CashRegisterDtos.CashRegisterResponse;
import com.estilospequenos.dto.CashRegisterDtos.MethodRow;
import com.estilospequenos.model.Exchange;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.PaymentMethod;
import com.estilospequenos.model.SaleChannel;
import com.estilospequenos.repository.ExchangeRepository;
import com.estilospequenos.repository.OrderRepository;
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

/**
 * Cierre de caja de un día: cuánto dinero entró por cada medio de pago,
 * separando ventas en el local (POS), diferencias de cambios y cobros de
 * pedidos online confirmados ese día.
 */
@Service
public class CashRegisterService {

    private static final Map<String, String> LABELS = Map.of(
            "CASH", "Efectivo",
            "TRANSFER", "Transferencia",
            "QR_TRANSFER", "QR de transferencia",
            "QR_CARD", "Tarjeta",
            "", "Sin especificar");
    /** Orden de las filas. */
    private static final List<String> ORDER = List.of("CASH", "TRANSFER", "QR_TRANSFER", "QR_CARD", "");

    private final OrderRepository orderRepo;
    private final ExchangeRepository exchangeRepo;
    private final ZoneId zone = ZoneId.systemDefault();

    public CashRegisterService(OrderRepository orderRepo, ExchangeRepository exchangeRepo) {
        this.orderRepo = orderRepo;
        this.exchangeRepo = exchangeRepo;
    }

    @Transactional(readOnly = true)
    public CashRegisterResponse forDay(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now();
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();

        // método -> [local, exchanges, online]
        Map<String, BigDecimal[]> acc = new LinkedHashMap<>();
        for (String m : ORDER) acc.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});

        List<Order> orders = orderRepo
                .findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(OrderStatus.PROCESADO, from, to);
        for (Order o : orders) {
            int idx = o.getChannel() == SaleChannel.LOCAL ? 0 : 2;
            acc.get(methodKey(o.getPaymentMethod()))[idx] =
                    acc.get(methodKey(o.getPaymentMethod()))[idx].add(o.getTotal());
        }

        for (Exchange e : exchangeRepo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)) {
            if (e.getDifference().signum() > 0) {
                acc.get(methodKey(e.getPaymentMethod()))[1] =
                        acc.get(methodKey(e.getPaymentMethod()))[1].add(e.getDifference());
            }
        }

        List<MethodRow> rows = new ArrayList<>();
        BigDecimal tl = BigDecimal.ZERO, te = BigDecimal.ZERO, to2 = BigDecimal.ZERO;
        for (String m : ORDER) {
            BigDecimal[] v = acc.get(m);
            BigDecimal rowTotal = v[0].add(v[1]).add(v[2]);
            if (rowTotal.signum() == 0) continue; // no mostrar medios sin movimiento
            rows.add(new MethodRow(m, LABELS.get(m), v[0], v[1], v[2], rowTotal));
            tl = tl.add(v[0]); te = te.add(v[1]); to2 = to2.add(v[2]);
        }

        MethodRow total = new MethodRow("", "Total", tl, te, to2, tl.add(te).add(to2));
        return new CashRegisterResponse(day.toString(), rows, total);
    }

    private static String methodKey(PaymentMethod m) {
        return m == null ? "" : m.name();
    }
}
