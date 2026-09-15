package com.saasweb.repository;

import com.saasweb.model.PlatformMailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformMailSettingsRepository extends JpaRepository<PlatformMailSettings, String> {
}
