package com.estilospequenos.repository;

import com.estilospequenos.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<AdminUser> findAllByOrderByCreatedAtAsc();
    long countByRoleId(String roleId);
    long countByEnabledTrue();
}
