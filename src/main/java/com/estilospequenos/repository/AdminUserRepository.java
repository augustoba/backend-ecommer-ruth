package com.estilospequenos.repository;

import com.estilospequenos.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
    Optional<AdminUser> findByDni(String dni);
    boolean existsByDniIgnoreCase(String dni);
    boolean existsByEmailIgnoreCase(String email);
    List<AdminUser> findAllByOrderByCreatedAtAsc();
    long countByRoleId(String roleId);
    long countByEnabledTrue();
}
