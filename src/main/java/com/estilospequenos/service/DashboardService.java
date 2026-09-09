package com.estilospequenos.service;

import com.estilospequenos.dto.DashboardDtos.DashboardResponse;
import com.estilospequenos.dto.DashboardDtos.LowStockItem;
import com.estilospequenos.dto.DashboardDtos.MonthSummary;
import com.estilospequenos.dto.DashboardDtos.ProductCounts;
import com.estilospequenos.dto.DashboardDtos.RecentOrder;
import com.estilospequenos.dto.MetricsDtos.Totals;
import com.estilospequenos.model.Order;
import com.estilospequenos.model.OrderStatus;
import com.estilospequenos.model.Product;
import com.estilospequenos.model.SizeStock;
import com.estilospequenos.repository.OrderRepository;
import com.estilospequenos.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        long pending = orderRepo.countByStatus(OrderStatus.PENDIENTE);

        LocalDate today = LocalDate.now();
        Totals month = metricsService.rangeTotals(today.withDayOfMonth(1), today);

        long active = productRepo.countByActiveTrueAndDeletedFalse();
        long total = productRepo.countByDeletedFalse();

        List<RecentOrder> recent = orderRepo.findAllByOrderByCreatedAtDesc().stream()
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

    /** Talles de productos activos cuyo stock está en o por debajo de su umbral. */
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        List<LowStockItem> items = new ArrayList<>();
        for (Product p : productRepo.findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc()) {
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
