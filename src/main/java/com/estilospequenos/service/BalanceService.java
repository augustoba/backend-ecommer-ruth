package com.estilospequenos.service;

import com.estilospequenos.dto.BalanceDtos.BalanceResponse;
import com.estilospequenos.dto.BalanceDtos.MonthBalance;
import com.estilospequenos.dto.MetricsDtos.Totals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Ganancia = ventas − costo de mercadería − gastos. No depende de si se
 * factura o no: se calcula sobre pedidos {@code PROCESADO} (ver
 * {@link MetricsService#rangeTotals}).
 */
@Service
public class BalanceService {

    private final MetricsService metricsService;
    private final ExpenseService expenseService;

    public BalanceService(MetricsService metricsService, ExpenseService expenseService) {
        this.metricsService = metricsService;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public BalanceResponse compute(LocalDate from, LocalDate to) {
        Totals totals = metricsService.rangeTotals(from, to);
        BigDecimal expenses = expenseService.totalForRange(from, to);
        BigDecimal grossProfit = totals.revenue().subtract(totals.cost());
        BigDecimal netResult = grossProfit.subtract(expenses);
        return new BalanceResponse(from.toString(), to.toString(), totals.revenue(), totals.cost(),
                grossProfit, expenses, netResult, totals.costDataComplete());
    }

    /** Serie mensual continua del año (hasta el mes actual si es el año en curso). */
    @Transactional(readOnly = true)
    public List<MonthBalance> compareYear(int year) {
        int lastMonth = year == LocalDate.now().getYear() ? LocalDate.now().getMonthValue() : 12;
        List<MonthBalance> out = new ArrayList<>();
        for (int m = 1; m <= lastMonth; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();
            Totals totals = metricsService.rangeTotals(from, to);
            BigDecimal expenses = expenseService.totalForRange(from, to);
            BigDecimal grossProfit = totals.revenue().subtract(totals.cost());
            BigDecimal netResult = grossProfit.subtract(expenses);
            out.add(new MonthBalance(String.format("%d-%02d", year, m), totals.revenue(), totals.cost(),
                    expenses, netResult));
        }
        return out;
    }
}
