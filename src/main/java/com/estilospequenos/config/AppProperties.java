package com.estilospequenos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuración de la app bajo el prefijo `app.*` (ver application.yml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Cors cors = new Cors();
    private Seed seed = new Seed();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 720;
    }

    /**
     * Credenciales del admin INICIAL: solo se usan para sembrar el primer
     * usuario en la tabla `admin_user` (con la contraseña hasheada) si todavía
     * no hay ninguno. Después el login valida contra la tabla, no contra esto.
     */
    @Getter
    @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "ruth123";
        /** Frase de recuperación inicial (cambiala desde /admin/cuenta). */
        private String recoveryPhrase = "frase-de-recuperacion-cambiar";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");
    }

    @Getter
    @Setter
    public static class Seed {
        private boolean enabled = true;
    }
}
