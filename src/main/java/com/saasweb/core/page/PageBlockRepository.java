package com.saasweb.core.page;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageBlockRepository extends JpaRepository<PageBlock, String> {
    Optional<PageBlock> findByIdAndTenantId(String id, String tenantId);
    List<PageBlock> findByTenantIdAndPageTypeOrderByPositionAsc(String tenantId, String pageType);
    List<PageBlock> findByTenantIdAndPageTypeAndVisibleTrueOrderByPositionAsc(String tenantId, String pageType);
}
