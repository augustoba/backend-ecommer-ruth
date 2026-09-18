package com.estilospequenos.dto;

import java.math.BigDecimal;

public final class BalanceDtos {

    private BalanceDtos() {}

    /**
     * Balance de un período: `netResult` = `revenue - cost - expenses`.
     * `costDataComplete` = false si alguna venta del período no tiene costo
     * cargado (margen parcial, no falso).
     */
    public record BalanceResponse(
            String from, String to,
            BigDecimal revenue, BigDecimal cost, BigDecimal grossProfit,
            BigDecimal expenses, BigDecimal netResult, boolean costDataComplete
    ) {}

    /** Un mes de la comparativa anual — para el gráfico rojo/verde mes a mes. */
    public record MonthBalance(
            String month, BigDecimal revenue, BigDecimal cost, BigDecimal expenses, BigDecimal netResult
    ) {}
}
