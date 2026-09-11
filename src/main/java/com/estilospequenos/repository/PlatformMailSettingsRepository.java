package com.estilospequenos.repository;

import com.estilospequenos.model.PlatformMailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformMailSettingsRepository extends JpaRepository<PlatformMailSettings, String> {
}
