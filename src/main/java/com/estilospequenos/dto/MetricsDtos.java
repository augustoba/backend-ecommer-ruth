package com.estilospequenos.dto;

import java.math.BigDecimal;
import java.util.List;

/** Métricas de ventas del panel (`/api/admin/metrics`). */
public class MetricsDtos {

    public record MetricsResponse(
            String from,
            String to,
            String basis,
            Totals totals,
            List<MonthBucket> byMonth,
            List<ProductStat> topProducts,
            List<ProductStat> bottomProducts,
            GroupBreakdown byGroup) {}

    /** Totales del período. `revenue` = precio de lista × cantidad de las líneas aceptadas. */
    public record Totals(BigDecimal revenue, long units, long orders) {}

    /** Un mes del período (siempre continuo: se rellenan los meses sin ventas con 0). */
    public record MonthBucket(String month, BigDecimal revenue, long units, long orders) {}

    public record ProductStat(String productId, String productName, long units, BigDecimal revenue) {}

    /** Desglose por un grupo de parametría (por defecto "Tipo de prenda"). */
    public record GroupBreakdown(String groupId, String groupName, List<GroupRow> rows) {}

    public record GroupRow(String optionId, String label, long units, BigDecimal revenue) {}
}
