package com.saasweb.core.settings;

import com.saasweb.core.settings.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, String> {

    /**
     * Cualquier fila con Cloudinary ya cargado — hoy es una sola cuenta de
     * plataforma (ver javadoc de {@code SiteSettings.cloudinaryCloudName}),
     * así que sirve para copiarla a una tienda recién creada en
     * {@code SiteSettingsService.createFor}.
     */
    Optional<SiteSettings> findFirstByCloudinaryCloudNameIsNotNull();
}
