package com.estilospequenos.repository;

import com.estilospequenos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrueOrderByCreatedAtDesc();
    List<Product> findAllByOrderByCreatedAtDesc();
    List<Product> findBySupplierId(String supplierId);
    long countByActiveTrue();
}
