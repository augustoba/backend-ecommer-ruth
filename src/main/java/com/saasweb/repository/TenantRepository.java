package com.saasweb.repository;

import com.saasweb.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findFirstByActiveTrueOrderByCreatedAtAsc();
}
