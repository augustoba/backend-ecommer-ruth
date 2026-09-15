package com.saasweb.core.settings;

import com.saasweb.core.settings.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, String> {
}
