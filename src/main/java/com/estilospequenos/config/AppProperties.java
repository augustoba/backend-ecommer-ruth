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
    private Superadmin superadmin = new Superadmin();
    private Cors cors = new Cors();
    private Seed seed = new Seed();
    private LoginThrottle loginThrottle = new LoginThrottle();
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 720;
    }

    /**
     * Datos del admin INICIAL (la dueña de la tienda): solo se usan para sembrar
     * ese usuario en la tabla `admin_user` (con la contraseña hasheada) si
     * todavía no existe uno con ese DNI. Después el login valida contra la
     * tabla, no contra esto. Rol asignado: "Administrador" (no system).
     */
    @Getter
    @Setter
    public static class Admin {
        private String nombre = "Ruth";
        private String apellido = "Basaury";
        private String dni = "11111111";
        private String email = "ruth@gmail.com";
        private String password = "ruth123";
        /** Frase de recuperación inicial (cambiala desde /admin/cuenta). */
        private String recoveryPhrase = "frase-de-recuperacion-cambiar";
    }

    /**
     * Datos del superadmin INICIAL (el desarrollador/dueño de la plataforma):
     * se siembra con rol "Superadmin" (system=true, todos los permisos siempre)
     * si todavía no existe un usuario con ese DNI. En otro deploy (otro
     * ecommerce), sobreescribir estas variables de entorno con los datos reales.
     */
    @Getter
    @Setter
    public static class Superadmin {
        private String nombre = "Augusto";
        private String apellido = "Basaury";
        private String dni = "33756194";
        private String email = "basauryaugusto@gmail.com";
        private String password = "augusto123";
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

    /**
     * Rate-limiting del login / recuperación de cuenta (ver LoginAttemptService).
     * Tras {@code maxAttempts} fallos dentro de {@code windowMinutes}, esa IP
     * (y ese usuario) quedan bloqueados {@code lockMinutes}.
     */
    @Getter
    @Setter
    public static class LoginThrottle {
        private boolean enabled = true;
        private int maxAttempts = 5;
        private long windowMinutes = 15;
        private long lockMinutes = 15;
    }

    /**
     * Valores INICIALES del servicio de mail (SMTP), usados sólo para sembrar
     * {@code PlatformMailSettings} la primera vez. Después se edita desde el
     * panel (superadmin) sin necesidad de redeploy.
     */
    @Getter
    @Setter
    public static class Mail {
        private String host = "smtp-relay.brevo.com";
        private int port = 587;
        private String username = "changeme@smtp-brevo.com";
        private String password = "changeme";
        private String fromAddress = "no-responder@estilospequenos.com";
    }
}
