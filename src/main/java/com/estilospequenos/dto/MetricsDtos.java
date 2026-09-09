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
            /** Desglose por canal: cuánto se vendió por la web y cuánto en el local. */
            ChannelBreakdown byChannel,
            List<MonthBucket> byMonth,
            List<ProductStat> topProducts,
            List<ProductStat> bottomProducts,
            GroupBreakdown byGroup) {}

    /** Totales del período. `revenue` = precio de lista × cantidad de las líneas aceptadas. */
    public record Totals(BigDecimal revenue, long units, long orders) {}

    /** Ventas online (checkout) vs en el local (POS). */
    public record ChannelBreakdown(Totals web, Totals local) {}

    /** Un mes del período (siempre continuo: se rellenan los meses sin ventas con 0). */
    public record MonthBucket(String month, BigDecimal revenue, long units, long orders) {}

    public record ProductStat(String productId, String productName, long units, BigDecimal revenue) {}

    /** Desglose por un grupo de parametría (por defecto "Tipo de prenda"). */
    public record GroupBreakdown(String groupId, String groupName, List<GroupRow> rows) {}

    public record GroupRow(String optionId, String label, long units, BigDecimal revenue) {}

    // --- Comparativas del año (`/api/admin/metrics/comparison`) ---

    public record ComparisonResponse(
            int year,
            /** Ventana de días del mes que se compara (semana en curso). */
            WeekWindow week,
            /** Venta total mes a mes del año (hasta el mes actual). */
            List<PeriodStat> monthly,
            /** Ventas del mismo tramo de días (`week`) mes a mes. */
            List<PeriodStat> weekly) {}

    public record WeekWindow(int weekOfMonth, int dayFrom, int dayTo) {}

    public record PeriodStat(String month, BigDecimal revenue, long units, long orders) {}
}
