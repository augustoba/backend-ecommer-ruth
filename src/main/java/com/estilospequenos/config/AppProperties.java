package com.estilospequenos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuración de la app bajo el prefijo `app.*` (ver application.yml). */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Superadmin superadmin = new Superadmin();
    private Cors cors = new Cors();
    private Seed seed = new Seed();
    private LoginThrottle loginThrottle = new LoginThrottle();
    private Mail mail = new Mail();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Superadmin getSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(Superadmin superadmin) {
        this.superadmin = superadmin;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public LoginThrottle getLoginThrottle() {
        return loginThrottle;
    }

    public void setLoginThrottle(LoginThrottle loginThrottle) {
        this.loginThrottle = loginThrottle;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public static class Jwt {
        private String secret;
        private long expirationMinutes = 720;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    /**
     * Datos del admin INICIAL (la dueña de la tienda): solo se usan para sembrar
     * ese usuario en la tabla `admin_user` (con la contraseña hasheada) si
     * todavía no existe uno con ese DNI. Después el login valida contra la
     * tabla, no contra esto. Rol asignado: "Administrador" (no system).
     */
    public static class Admin {
        private String nombre = "Ruth";
        private String apellido = "Basaury";
        private String dni = "11111111";
        private String email = "ruth@gmail.com";
        private String password = "ruth123";

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }

        public String getDni() {
            return dni;
        }

        public void setDni(String dni) {
            this.dni = dni;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Datos del superadmin INICIAL (el desarrollador/dueño de la plataforma):
     * se siembra con rol "Superadmin" (system=true, todos los permisos siempre)
     * y con el flag {@code superAdmin=true} (acceso aparte a Cloudinary/mail) si
     * todavía no existe un usuario con ese DNI. En otro deploy (otro ecommerce),
     * sobreescribir estas variables de entorno con los datos reales.
     */
    public static class Superadmin {
        private String nombre = "Augusto";
        private String apellido = "Basaury";
        private String dni = "33756194";
        private String email = "basauryaugusto@gmail.com";
        private String password = "augusto123";

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }

        public String getDni() {
            return dni;
        }

        public void setDni(String dni) {
            this.dni = dni;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Seed {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Rate-limiting del login / recuperación de cuenta (ver LoginAttemptService).
     * Tras {@code maxAttempts} fallos dentro de {@code windowMinutes}, esa IP
     * (y ese usuario) quedan bloqueados {@code lockMinutes}.
     */
    public static class LoginThrottle {
        private boolean enabled = true;
        private int maxAttempts = 5;
        private long windowMinutes = 15;
        private long lockMinutes = 15;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public long getLockMinutes() {
            return lockMinutes;
        }

        public void setLockMinutes(long lockMinutes) {
            this.lockMinutes = lockMinutes;
        }
    }

    /**
     * Valores INICIALES del servicio de mail (SMTP), usados sólo para sembrar
     * {@code PlatformMailSettings} la primera vez. Después se edita desde el
     * panel (superadmin) sin necesidad de redeploy.
     */
    public static class Mail {
        private String host = "smtp-relay.brevo.com";
        private int port = 587;
        private String username = "changeme@smtp-brevo.com";
        private String password = "changeme";
        private String fromAddress = "no-responder@estilospequenos.com";

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
}
