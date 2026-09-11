package com.estilospequenos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciales del servicio de mail (SMTP), una sola fila igual que
 * {@link SiteSettings}. Editable sólo por el superadmin desde el panel — así
 * en otro deploy (otro ecommerce) se configura sin tocar el servidor.
 */
@Entity
@Table(name = "platform_mail_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlatformMailSettings {

    public static final String SINGLETON_ID = "config";

    @Id
    private String id = SINGLETON_ID;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String username;

    /** Clave SMTP. Nunca se devuelve en texto plano por la API (ver PlatformMailSettingsResponse). */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fromAddress;
}
