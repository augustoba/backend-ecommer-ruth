package com.saasweb.core.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, String> {
    List<ExpenseBudget> findByTenantId(String tenantId);

    Optional<ExpenseBudget> findByIdAndTenantId(String id, String tenantId);

    Optional<ExpenseBudget> findByTenantIdAndCategoryOptionId(String tenantId, String categoryOptionId);

    /** Ver TenantDeletionService. */
    void deleteAllByTenantId(String tenantId);
}
