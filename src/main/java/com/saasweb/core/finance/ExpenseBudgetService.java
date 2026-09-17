package com.saasweb.core.finance;

import com.saasweb.common.ResourceNotFoundException;
import com.saasweb.common.TenantContext;
import com.saasweb.core.finance.ExpenseBudgetDtos.BudgetRequest;
import com.saasweb.core.finance.ExpenseBudgetDtos.BudgetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ExpenseBudgetService {

    private final ExpenseBudgetRepository repo;
    private final ExpenseService expenseService;

    public ExpenseBudgetService(ExpenseBudgetRepository repo, ExpenseService expenseService) {
        this.repo = repo;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseBudget> findAll() {
        return repo.findByTenantId(TenantContext.getTenantId());
    }

    /** Presupuesto vs. gastado en lo que va del mes actual, por categoría (ítem 21). */
    @Transactional(readOnly = true)
    public List<BudgetStatus> statusForCurrentMonth() {
        YearMonth month = YearMonth.now();
        LocalDate from = month.atDay(1);
        LocalDate to = LocalDate.now();
        Map<String, BigDecimal> spent = expenseService.totalsByCategoryForRange(from, to);

        List<BudgetStatus> out = new java.util.ArrayList<>();
        for (ExpenseBudget b : findAll()) {
            BigDecimal spentAmount = spent.getOrDefault(b.getCategoryOptionId(), BigDecimal.ZERO);
            out.add(new BudgetStatus(b.getId(), b.getCategoryOptionId(), b.getMonthlyAmount(), spentAmount,
                    spentAmount.compareTo(b.getMonthlyAmount()) > 0));
        }
        return out;
    }

    /** Crea o actualiza el presupuesto de una categoría (una fila por categoría). */
    public ExpenseBudget upsert(BudgetRequest req) {
        String tenantId = TenantContext.getTenantId();
        ExpenseBudget b = repo.findByTenantIdAndCategoryOptionId(tenantId, req.categoryOptionId())
                .orElseGet(() -> {
                    ExpenseBudget nb = new ExpenseBudget();
                    nb.setId(UUID.randomUUID().toString());
                    nb.setTenantId(tenantId);
                    nb.setCategoryOptionId(req.categoryOptionId());
                    return nb;
                });
        b.setMonthlyAmount(req.monthlyAmount());
        return repo.save(b);
    }

    public void delete(String id) {
        ExpenseBudget b = repo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Presupuesto", id));
        repo.delete(b);
    }
}
