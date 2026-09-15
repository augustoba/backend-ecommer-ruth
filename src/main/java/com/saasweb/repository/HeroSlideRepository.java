package com.saasweb.repository;

import com.saasweb.model.HeroSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HeroSlideRepository extends JpaRepository<HeroSlide, String> {
    Optional<HeroSlide> findByIdAndTenantId(String id, String tenantId);
    List<HeroSlide> findByTenantIdOrderByPositionAsc(String tenantId);
    long countByTenantId(String tenantId);
}
