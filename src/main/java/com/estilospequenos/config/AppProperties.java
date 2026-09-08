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

    @Getter
    @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "cambiar-esta-clave";
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
