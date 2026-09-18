package com.estilospequenos.service;

import com.estilospequenos.common.ResourceNotFoundException;
import com.estilospequenos.dto.ExpenseDtos.ExpenseRequest;
import com.estilospequenos.model.Expense;
import com.estilospequenos.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository repo;

    public ExpenseService(ExpenseRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Expense> search(LocalDate from, LocalDate to) {
        if (from == null && to == null) return repo.findByOrderByDateDesc();
        LocalDate f = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate t = to != null ? to : LocalDate.now();
        return repo.findByDateBetweenOrderByDateDesc(f, t);
    }

    @Transactional(readOnly = true)
    public Expense get(String id) {
        return repo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Gasto", id));
    }

    /** Suma de gastos en el rango — usado por {@code BalanceService}. */
    @Transactional(readOnly = true)
    public BigDecimal totalForRange(LocalDate from, LocalDate to) {
        return search(from, to).stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gasto acumulado por categoría en el rango — usado por {@code ExpenseBudgetService}. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> totalsByCategoryForRange(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense e : search(from, to)) {
            String cat = e.getCategoryOptionId() == null ? "" : e.getCategoryOptionId();
            byCategory.merge(cat, e.getAmount(), BigDecimal::add);
        }
        return byCategory;
    }

    public Expense create(ExpenseRequest req) {
        Expense e = new Expense();
        e.setId(UUID.randomUUID().toString());
        apply(e, req);
        return repo.save(e);
    }

    public Expense update(String id, ExpenseRequest req) {
        Expense e = get(id);
        apply(e, req);
        return repo.save(e);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    private void apply(Expense e, ExpenseRequest req) {
        e.setDate(req.date());
        e.setCategoryOptionId(blankToNull(req.categoryOptionId()));
        e.setAmount(req.amount());
        e.setDescription(blankToNull(req.description()));
        e.setRepeatMonthly(req.repeatMonthly() != null && req.repeatMonthly());
        // Cabeza de una serie recurrente nueva: se autoasigna su propio id.
        if (e.isRepeatMonthly() && e.getRecurringGroupId() == null) {
            e.setRecurringGroupId(e.getId());
        }
    }

    /**
     * Genera la instancia del mes actual de cada serie "repetir cada mes" que
     * todavía no la tenga — llamado por {@link ExpenseRecurrenceScheduler} una
     * vez por mes. Si el scheduler se salteó más de un mes (ej. el servidor
     * estuvo caído), sólo genera la del mes actual, no rellena los meses
     * intermedios — simplificación deliberada.
     */
    public void generateRecurringForCurrentMonth() {
        YearMonth currentMonth = YearMonth.now();

        Map<String, Expense> latestByGroup = new LinkedHashMap<>();
        for (Expense e : repo.findByRepeatMonthlyTrue()) {
            String key = e.getRecurringGroupId();
            Expense current = latestByGroup.get(key);
            if (current == null || e.getDate().isAfter(current.getDate())) {
                latestByGroup.put(key, e);
            }
        }

        for (Expense latest : latestByGroup.values()) {
            if (!YearMonth.from(latest.getDate()).isBefore(currentMonth)) continue; // ya está al día

            Expense next = new Expense();
            next.setId(UUID.randomUUID().toString());
            next.setDate(currentMonth.atDay(Math.min(latest.getDate().getDayOfMonth(), currentMonth.lengthOfMonth())));
            next.setCategoryOptionId(latest.getCategoryOptionId());
            next.setAmount(latest.getAmount());
            next.setDescription(latest.getDescription());
            next.setRepeatMonthly(true);
            next.setRecurringGroupId(latest.getRecurringGroupId());
            repo.save(next);
        }
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
