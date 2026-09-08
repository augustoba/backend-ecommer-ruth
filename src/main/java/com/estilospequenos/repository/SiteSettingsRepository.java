package com.estilospequenos.repository;

import com.estilospequenos.model.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, String> {
}
