package com.estilospequenos.repository;

import com.estilospequenos.model.ExpenseBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, String> {
    Optional<ExpenseBudget> findByCategoryOptionId(String categoryOptionId);
}
