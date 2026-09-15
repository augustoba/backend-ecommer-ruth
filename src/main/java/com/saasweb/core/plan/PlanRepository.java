package com.saasweb.core.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, String> {
    Optional<Plan> findBySlug(String slug);
}
