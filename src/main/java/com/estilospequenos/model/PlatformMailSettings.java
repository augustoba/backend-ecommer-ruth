package com.estilospequenos.model;

import jakarta.persistence.*;

/**
 * Credenciales del servicio de mail (SMTP), una sola fila igual que
 * {@link SiteSettings}. Editable sólo por el superadmin desde el panel — así
 * en otro deploy (otro ecommerce) se configura sin tocar el servidor.
 */
@Entity
@Table(name = "platform_mail_settings")
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
