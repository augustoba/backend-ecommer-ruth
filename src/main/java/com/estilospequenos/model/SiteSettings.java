package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del local editables desde el panel (nombre, WhatsApp, "sobre nosotros",
 * redes). Una sola fila (id fijo). Así el dueño/a puede cambiar el número de
 * WhatsApp o las redes sin redesplegar nada.
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
public class SiteSettings {

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

    @Column(nullable = false)
    private String storeName;

    /** Número de WhatsApp en formato internacional sin +, espacios ni 15. */
    @Column(nullable = false)
    private String whatsappNumber;

    @Column(length = 2000)
    private String aboutText;

    /** Usuario de Instagram, sin @. */
    private String instagram;

    private String facebookUrl;
}
