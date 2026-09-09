package com.estilospequenos.repository;

import com.estilospequenos.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    Optional<Role> findFirstBySystemTrue();
    List<Role> findAllByOrderBySystemDescNameAsc();
}
