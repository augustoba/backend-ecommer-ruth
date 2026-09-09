package com.estilospequenos.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cierre de caja de un día (`GET /api/admin/cash-register?date=YYYY-MM-DD`). */
public final class CashRegisterDtos {

    private CashRegisterDtos() {}

    public record CashRegisterResponse(
            String date,
            List<MethodRow> rows,
            /** Suma de todas las filas. */
            MethodRow total
    ) {}

    /**
     * Total de un medio de pago en el día, abierto por origen:
     *  - `local`: ventas cargadas en el local (POS).
     *  - `exchanges`: diferencias cobradas en cambios de prenda.
     *  - `online`: pedidos de la web confirmados hoy con ese medio de pago.
     */
    public record MethodRow(
            String method,       // "CASH" | "TRANSFER" | "QR_TRANSFER" | "QR_CARD" | "" (sin especificar)
            String label,
            BigDecimal local,
            BigDecimal exchanges,
            BigDecimal online,
            BigDecimal total
    ) {}
}
