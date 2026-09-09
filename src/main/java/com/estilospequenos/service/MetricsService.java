package com.estilospequenos.service;

import com.estilospequenos.dto.MetricsDtos.ComparisonResponse;
import com.estilospequenos.dto.MetricsDtos.GroupBreakdown;
import com.estilospequenos.dto.MetricsDtos.GroupRow;
import com.estilospequenos.dto.MetricsDtos.MetricsResponse;
import com.estilospequenos.dto.MetricsDtos.MonthBucket;
import com.estilospequenos.dto.MetricsDtos.PeriodStat;
import com.estilospequenos.dto.MetricsDtos.ProductStat;
import com.estilospequenos.dto.MetricsDtos.Totals;
import com.estilospequenos.dto.MetricsDtos.WeekWindow;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderLine;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.ParamGroup;
import com.estilospequenos.model.ParamOption;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.ProductParam;
import com.estilospequenos.model.Exchange;
import com.estilospequenos.model.Supplier;
import com.estilospequenos.repository.ExchangeRepository;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ParamRepository;
import com.estilospequenos.repository.ProductRepository;
import com.estilospequenos.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Métricas de ventas: se calculan sobre las líneas <b>aceptadas</b> de los
 * pedidos <b>PROCESADO</b>, tomando la fecha en que se procesó el pedido
 * (`processedAt`). `revenue` = precio de lista × cantidad (no aplica el
 * descuento del pedido, que es a nivel total).
 */
@Service
public class MetricsService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_GROUP = "grp-tipo";
    private static final int TOP_N = 10;

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final ParamRepository paramRepo;
    private final SupplierRepository supplierRepo;
    private final ExchangeRepository exchangeRepo;
    private final ZoneId zone = ZoneId.systemDefault();

    public MetricsService(OrderRepository orderRepo, ProductRepository productRepo, ParamRepository paramRepo,
                          SupplierRepository supplierRepo, ExchangeRepository exchangeRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paramRepo = paramRepo;
        this.supplierRepo = supplierRepo;
        this.exchangeRepo = exchangeRepo;
    }

    /** Totales de ventas (líneas aceptadas de pedidos PROCESADO) en un rango. Usado por el dashboard. */
    @Transactional(readOnly = true)
    public Totals rangeTotals(LocalDate from, LocalDate to) {
        Instant fromI = from.atStartOfDay(zone).toInstant();
        Instant toI = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<Order> orders = orderRepo
                .findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(OrderStatus.PROCESADO, fromI, toI);
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;
        for (Order o : orders) {
            for (OrderLine l : o.getLines()) {
                if (!l.isAccepted()) continue;
                revenue = revenue.add(l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())));
                units += l.getQuantity();
            }
        }
        return new Totals(revenue, units, orders.size());
    }

    @Transactional(readOnly = true)
    public MetricsResponse compute(LocalDate from, LocalDate to, String groupBy) {
        Instant fromI = from.atStartOfDay(zone).toInstant();
        Instant toI = to.plusDays(1).atStartOfDay(zone).toInstant();

        List<Order> orders = orderRepo
                .findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(OrderStatus.PROCESADO, fromI, toI);

        Map<String, Product> productsById = new LinkedHashMap<>();
        for (Product p : productRepo.findAll()) productsById.put(p.getId(), p);

        String groupId = (groupBy == null || groupBy.isBlank()) ? DEFAULT_GROUP : groupBy.trim();
        ParamGroup group = paramRepo.findById(groupId).orElse(null);

        // --- acumuladores ---
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalUnits = 0;

        Map<String, Acc> byMonth = new LinkedHashMap<>();
        Map<String, ProdAcc> byProduct = new LinkedHashMap<>();
        Map<String, Acc> byOption = new LinkedHashMap<>(); // optionId ("" = sin opción) -> acc
        Map<String, Acc> bySize = new LinkedHashMap<>();
        Map<String, Acc> bySupplier = new LinkedHashMap<>(); // supplierId ("" = sin proveedor) -> acc
        Acc webAcc = new Acc();
        Acc localAcc = new Acc();

        for (Order o : orders) {
            String month = YearMonth.from(o.getProcessedAt().atZone(zone)).format(MONTH);
            Acc monthAcc = byMonth.computeIfAbsent(month, k -> new Acc());
            Acc channelAcc = o.getChannel() == com.estilospequenos.model.SaleChannel.LOCAL ? localAcc : webAcc;
            boolean countedOrder = false;

            for (OrderLine l : o.getLines()) {
                if (!l.isAccepted()) continue;
                BigDecimal lineRevenue = l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()));
                long qty = l.getQuantity();

                totalRevenue = totalRevenue.add(lineRevenue);
                totalUnits += qty;

                monthAcc.revenue = monthAcc.revenue.add(lineRevenue);
                monthAcc.units += qty;
                channelAcc.revenue = channelAcc.revenue.add(lineRevenue);
                channelAcc.units += qty;
                countedOrder = true;

                ProdAcc pa = byProduct.computeIfAbsent(l.getProductId(),
                        k -> new ProdAcc(l.getProductName()));
                pa.revenue = pa.revenue.add(lineRevenue);
                pa.units += qty;

                Acc sa = bySize.computeIfAbsent(l.getSize() == null ? "" : l.getSize(), k -> new Acc());
                sa.revenue = sa.revenue.add(lineRevenue);
                sa.units += qty;

                Product prod = productsById.get(l.getProductId());
                String supId = prod != null && prod.getSupplierId() != null ? prod.getSupplierId() : "";
                Acc supAcc = bySupplier.computeIfAbsent(supId, k -> new Acc());
                supAcc.revenue = supAcc.revenue.add(lineRevenue);
                supAcc.units += qty;

                for (String optionId : optionIdsFor(productsById.get(l.getProductId()), groupId)) {
                    Acc oa = byOption.computeIfAbsent(optionId, k -> new Acc());
                    oa.revenue = oa.revenue.add(lineRevenue);
                    oa.units += qty;
                }
            }
            if (countedOrder) {
                monthAcc.orders++;
                channelAcc.orders++;
            }
        }

        // --- Cambios: la diferencia cobrada cuenta como facturación (local). No suma unidades ni pedidos. ---
        for (Exchange e : exchangeRepo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(fromI, toI)) {
            if (e.getDifference().signum() <= 0) continue;
            BigDecimal diff = e.getDifference();
            totalRevenue = totalRevenue.add(diff);
            localAcc.revenue = localAcc.revenue.add(diff);
            String month = YearMonth.from(e.getCreatedAt().atZone(zone)).format(MONTH);
            Acc ma = byMonth.computeIfAbsent(month, k -> new Acc());
            ma.revenue = ma.revenue.add(diff);
        }

        return new MetricsResponse(
                from.toString(),
                to.toString(),
                "processedAt",
                new Totals(totalRevenue, totalUnits, orders.size()),
                new com.estilospequenos.dto.MetricsDtos.ChannelBreakdown(
                        new Totals(webAcc.revenue, webAcc.units, webAcc.orders),
                        new Totals(localAcc.revenue, localAcc.units, localAcc.orders)),
                monthSeries(from, to, byMonth),
                topProducts(byProduct),
                bottomProducts(byProduct, productsById),
                groupBreakdown(group, groupId, byOption),
                sizeRows(bySize),
                supplierRows(bySupplier));
    }

    private List<com.estilospequenos.dto.MetricsDtos.GroupRow> sizeRows(Map<String, Acc> bySize) {
        return bySize.entrySet().stream()
                .filter(en -> en.getValue().units > 0)
                .sorted(java.util.Comparator.comparingLong((Map.Entry<String, Acc> en) -> en.getValue().units).reversed())
                .map(en -> new com.estilospequenos.dto.MetricsDtos.GroupRow(
                        en.getKey().isEmpty() ? null : en.getKey(),
                        en.getKey().isEmpty() ? "Sin talle" : "Talle " + en.getKey(),
                        en.getValue().units, en.getValue().revenue))
                .toList();
    }

    private List<com.estilospequenos.dto.MetricsDtos.GroupRow> supplierRows(Map<String, Acc> bySupplier) {
        Map<String, String> names = new LinkedHashMap<>();
        for (Supplier s : supplierRepo.findAll()) names.put(s.getId(), s.getName());
        return bySupplier.entrySet().stream()
                .filter(en -> en.getValue().units > 0)
                .sorted(java.util.Comparator.comparingLong((Map.Entry<String, Acc> en) -> en.getValue().units).reversed())
                .map(en -> new com.estilospequenos.dto.MetricsDtos.GroupRow(
                        en.getKey().isEmpty() ? null : en.getKey(),
                        en.getKey().isEmpty() ? "Sin proveedor" : names.getOrDefault(en.getKey(), en.getKey()),
                        en.getValue().units, en.getValue().revenue))
                .toList();
    }

    /**
     * Comparativas del año: venta total mes a mes, y ventas del mismo tramo de
     * días de cada mes (la "semana en curso" del mes actual, tomando bloques de
     * 7 días: 1–7, 8–14, 15–21, 22–28, 29–fin). El tramo se corta en el día de
     * hoy, así se compara "lo que va de la semana" contra el mismo punto de los
     * meses anteriores.
     */
    @Transactional(readOnly = true)
    public ComparisonResponse compareYear(Integer yearParam) {
        LocalDate today = LocalDate.now();
        int year = yearParam != null ? yearParam : today.getYear();
        int lastMonth = year == today.getYear() ? today.getMonthValue() : 12;

        int weekOfMonth = (today.getDayOfMonth() - 1) / 7 + 1;
        int dayFrom = (weekOfMonth - 1) * 7 + 1;
        int dayTo = Math.min(today.getDayOfMonth(), dayFrom + 6);

        Instant yearStart = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
        Instant yearEnd = LocalDate.of(year, 1, 1).plusYears(1).atStartOfDay(zone).toInstant();
        List<Order> orders = orderRepo
                .findByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(OrderStatus.PROCESADO, yearStart, yearEnd);

        Map<Integer, Acc> monthly = new LinkedHashMap<>();
        Map<Integer, Acc> weekly = new LinkedHashMap<>();

        for (Order o : orders) {
            LocalDate d = LocalDate.ofInstant(o.getProcessedAt(), zone);
            BigDecimal revenue = BigDecimal.ZERO;
            long units = 0;
            for (OrderLine l : o.getLines()) {
                if (!l.isAccepted()) continue;
                revenue = revenue.add(l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())));
                units += l.getQuantity();
            }
            if (units == 0) continue;

            add(monthly.computeIfAbsent(d.getMonthValue(), k -> new Acc()), revenue, units);
            if (d.getDayOfMonth() >= dayFrom && d.getDayOfMonth() <= dayTo) {
                add(weekly.computeIfAbsent(d.getMonthValue(), k -> new Acc()), revenue, units);
            }
        }

        return new ComparisonResponse(
                year,
                new WeekWindow(weekOfMonth, dayFrom, dayTo),
                periodSeries(year, lastMonth, monthly),
                periodSeries(year, lastMonth, weekly));
    }

    private static void add(Acc acc, BigDecimal revenue, long units) {
        acc.revenue = acc.revenue.add(revenue);
        acc.units += units;
        acc.orders++;
    }

    private List<PeriodStat> periodSeries(int year, int lastMonth, Map<Integer, Acc> byMonth) {
        List<PeriodStat> out = new ArrayList<>();
        for (int m = 1; m <= lastMonth; m++) {
            Acc a = byMonth.getOrDefault(m, new Acc());
            out.add(new PeriodStat(String.format("%d-%02d", year, m), a.revenue, a.units, a.orders));
        }
        return out;
    }

    /** Opciones del grupo pedido que tiene el producto. Vacío ("") si no tiene ninguna o el producto no existe. */
    private List<String> optionIdsFor(Product p, String groupId) {
        if (p == null) return List.of("");
        List<String> ids = new ArrayList<>();
        for (ProductParam pp : p.getParams()) {
            if (pp.getGroupId().equals(groupId)) ids.add(pp.getOptionId());
        }
        return ids.isEmpty() ? List.of("") : ids;
    }

    private List<MonthBucket> monthSeries(LocalDate from, LocalDate to, Map<String, Acc> byMonth) {
        List<MonthBucket> out = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            String key = cursor.format(MONTH);
            Acc a = byMonth.getOrDefault(key, new Acc());
            out.add(new MonthBucket(key, a.revenue, a.units, a.orders));
            cursor = cursor.plusMonths(1);
        }
        return out;
    }

    private List<ProductStat> topProducts(Map<String, ProdAcc> byProduct) {
        return byProduct.entrySet().stream()
                .filter(e -> e.getValue().units > 0)
                .sorted(Comparator
                        .comparingLong((Map.Entry<String, ProdAcc> e) -> e.getValue().units).reversed()
                        .thenComparing(e -> e.getValue().revenue, Comparator.reverseOrder()))
                .limit(TOP_N)
                .map(e -> new ProductStat(e.getKey(), e.getValue().name, e.getValue().units, e.getValue().revenue))
                .toList();
    }

    /** Los menos vendidos: incluye productos activos SIN ventas en el período (unidades 0). */
    private List<ProductStat> bottomProducts(Map<String, ProdAcc> byProduct, Map<String, Product> productsById) {
        Map<String, ProdAcc> all = new LinkedHashMap<>(byProduct);
        for (Product p : productsById.values()) {
            if (p.isActive()) all.computeIfAbsent(p.getId(), k -> new ProdAcc(p.getName()));
        }
        return all.entrySet().stream()
                .sorted(Comparator
                        .comparingLong((Map.Entry<String, ProdAcc> e) -> e.getValue().units)
                        .thenComparing(e -> e.getValue().revenue))
                .limit(TOP_N)
                .map(e -> new ProductStat(e.getKey(), e.getValue().name, e.getValue().units, e.getValue().revenue))
                .toList();
    }

    private GroupBreakdown groupBreakdown(ParamGroup group, String groupId, Map<String, Acc> byOption) {
        String groupName = group != null ? group.getName() : groupId;
        Map<String, String> labels = new LinkedHashMap<>();
        if (group != null) {
            for (ParamOption opt : group.getOptions()) labels.put(opt.getId(), opt.getLabel());
        }

        List<GroupRow> rows = new ArrayList<>();
        // primero las opciones del grupo en su orden, después "sin dato"
        for (Map.Entry<String, String> l : labels.entrySet()) {
            Acc a = byOption.getOrDefault(l.getKey(), new Acc());
            rows.add(new GroupRow(l.getKey(), l.getValue(), a.units, a.revenue));
        }
        Acc none = byOption.get("");
        if (none != null && none.units > 0) {
            rows.add(new GroupRow(null, "Sin dato", none.units, none.revenue));
        }
        // opciones que aparecen en ventas pero no están en el grupo (labels desconocidos)
        for (Map.Entry<String, Acc> e : byOption.entrySet()) {
            if (!e.getKey().isEmpty() && !labels.containsKey(e.getKey())) {
                rows.add(new GroupRow(e.getKey(), e.getKey(), e.getValue().units, e.getValue().revenue));
            }
        }
        rows.sort(Comparator.comparingLong(GroupRow::units).reversed());
        return new GroupBreakdown(groupId, groupName, rows);
    }

    private static final class Acc {
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;
        long orders = 0;
    }

    private static final class ProdAcc {
        final String name;
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;

        ProdAcc(String name) {
            this.name = name;
        }
    }
}
