package com.saasweb.core.dashboard;

import com.saasweb.common.TenantContext;
import com.saasweb.core.dashboard.DashboardDtos.DashboardResponse;
import com.saasweb.core.dashboard.DashboardDtos.LowStockItem;
import com.saasweb.core.dashboard.DashboardDtos.MonthSummary;
import com.saasweb.core.dashboard.DashboardDtos.ProductCounts;
import com.saasweb.core.dashboard.DashboardDtos.RecentOrder;
import com.saasweb.core.dashboard.MetricsDtos.Totals;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderStatus;
import com.saasweb.core.product.Product;
import com.saasweb.modules.ropa.SizeStock;
import com.saasweb.core.order.OrderRepository;
import com.saasweb.core.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    /** Umbral de stock bajo por defecto (unidades por talle) si el producto no define el suyo. */
    public static final int DEFAULT_LOW_STOCK = 3;
    private static final int RECENT_ORDERS = 6;

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final MetricsService metricsService;

    public DashboardService(OrderRepository orderRepo, ProductRepository productRepo, MetricsService metricsService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.metricsService = metricsService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        String tenantId = TenantContext.getTenantId();
        long pending = orderRepo.countByTenantIdAndStatus(tenantId, OrderStatus.PENDIENTE);

        LocalDate today = LocalDate.now();
        Totals month = metricsService.rangeTotals(today.withDayOfMonth(1), today);

        long active = productRepo.countByTenantIdAndActiveTrueAndDeletedFalse(tenantId);
        long total = productRepo.countByTenantIdAndDeletedFalse(tenantId);

        List<RecentOrder> recent = orderRepo.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .limit(RECENT_ORDERS)
                .map(o -> new RecentOrder(o.getId(), o.getCode(), o.getCustomerName(),
                        o.getTotal(), o.getStatus().name(), o.getCreatedAt()))
                .toList();

        List<LowStockItem> lowStock = lowStock();

        return new DashboardResponse(
                pending,
                new MonthSummary(month.revenue(), month.units(), month.orders()),
                new ProductCounts(active, total - active),
                recent,
                DEFAULT_LOW_STOCK,
                lowStock);
    }

    private static final DateTimeFormatter CAE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Pedidos con Factura ARCA aprobada cuyo CAE vence dentro de {@code days}
     * días (informativo — ver ítem 5 de la ronda de mejoras). `caeVencimiento`
     * viene de ARCA en formato `yyyyMMdd`.
     */
    @Transactional(readOnly = true)
    public List<DashboardDtos.ExpiringCaeItem> expiringCae(int days) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(days);
        List<DashboardDtos.ExpiringCaeItem> out = new ArrayList<>();
        for (Order o : orderRepo.findByTenantIdAndInvoiceCaeIsNotNull(TenantContext.getTenantId())) {
            if (o.getInvoiceCaeVencimiento() == null) continue;
            LocalDate vto;
            try {
                vto = LocalDate.parse(o.getInvoiceCaeVencimiento(), CAE_FMT);
            } catch (DateTimeParseException e) {
                continue;
            }
            if (!vto.isBefore(today) && !vto.isAfter(limit)) {
                out.add(new DashboardDtos.ExpiringCaeItem(o.getId(), o.getCode(), o.getInvoiceType(),
                        o.getInvoiceCaeVencimiento(), java.time.temporal.ChronoUnit.DAYS.between(today, vto)));
            }
        }
        out.sort(Comparator.comparingLong(DashboardDtos.ExpiringCaeItem::diasRestantes));
        return out;
    }

    /** Talles de productos activos cuyo stock está en o por debajo de su umbral. */
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        List<LowStockItem> items = new ArrayList<>();
        for (Product p : productRepo.findByTenantIdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(TenantContext.getTenantId())) {
            if (p.isDiscontinued()) continue; // el dueño/a marcó "no reponer"
            int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : DEFAULT_LOW_STOCK;
            for (SizeStock s : p.getSizeStocks()) {
                if (s.getStock() <= threshold) {
                    items.add(new LowStockItem(p.getId(), p.getName(), s.getSize(), s.getStock(), threshold));
                }
            }
        }
        items.sort(Comparator.comparingInt(LowStockItem::stock)
                .thenComparing(LowStockItem::productName));
        return items;
    }
}
