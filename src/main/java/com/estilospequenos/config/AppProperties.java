package com.estilospequenos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuración de la app bajo el prefijo `app.*` (ver application.yml). */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Cors cors = new Cors();
    private Seed seed = new Seed();
    private LoginThrottle loginThrottle = new LoginThrottle();

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
     * Credenciales del admin INICIAL: solo se usan para sembrar el primer
     * usuario en la tabla `admin_user` (con la contraseña hasheada) si todavía
     * no hay ninguno. Después el login valida contra la tabla, no contra esto.
     */
    public static class Admin {
        private String username = "admin";
        private String password = "ruth123";
        /** Frase de recuperación inicial (cambiala desde /admin/cuenta). */
        private String recoveryPhrase = "frase-de-recuperacion-cambiar";

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

        public String getRecoveryPhrase() {
            return recoveryPhrase;
        }

        public void setRecoveryPhrase(String recoveryPhrase) {
            this.recoveryPhrase = recoveryPhrase;
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
}
