package com.saasweb.platform;

import com.saasweb.platform.PlatformMailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformMailSettingsRepository extends JpaRepository<PlatformMailSettings, String> {
}
