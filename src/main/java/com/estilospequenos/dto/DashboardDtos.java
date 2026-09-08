package com.estilospequenos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Resumen del panel (`GET /api/admin/dashboard`). */
public class DashboardDtos {

    public record DashboardResponse(
            long pendingOrders,
            MonthSummary month,
            ProductCounts products,
            List<RecentOrder> recentOrders,
            /** Umbral global de stock bajo por defecto (unidades por talle). */
            int defaultLowStockThreshold,
            /** Talles por reponer, del más crítico al menos. */
            List<LowStockItem> lowStock) {}

    public record MonthSummary(BigDecimal revenue, long units, long orders) {}

    public record ProductCounts(long active, long hidden) {}

    public record RecentOrder(
            String id, String code, String customerName,
            BigDecimal total, String status, Instant createdAt) {}

    public record LowStockItem(
            String productId, String productName, String size, int stock, int threshold) {}
}
