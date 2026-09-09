package com.estilospequenos.service;

import com.estilospequenos.common.TooManyRequestsException;
import com.estilospequenos.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiting en memoria para el login y la recuperación de cuenta.
 *
 * <p>Cuenta los intentos fallidos por clave (IP y, aparte, usuario) dentro de
 * una ventana móvil. Al pasar el máximo, bloquea esa clave por un rato y
 * responde 429. Un login exitoso limpia las claves.</p>
 *
 * <p>El estado vive en un {@link ConcurrentHashMap} — alcanza para un backend
 * de una instancia y un solo admin. Si algún día hay varias instancias, esto
 * habría que moverlo a Redis o similar.</p>
 */
@Service
public class LoginAttemptService {

    private final AppProperties.LoginThrottle cfg;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(AppProperties props) {
        this.cfg = props.getLoginThrottle();
    }

    /** Tira 429 si la IP o el usuario están bloqueados por intentos fallidos. */
    public void assertNotBlocked(String ip, String username) {
        if (!cfg.isEnabled()) return;
        Instant now = Instant.now();
        checkKey(ipKey(ip), now);
        checkKey(userKey(username), now);
    }

    /** Registra un intento fallido para la IP y el usuario. */
    public void recordFailure(String ip, String username) {
        if (!cfg.isEnabled()) return;
        Instant now = Instant.now();
        bump(ipKey(ip), now);
        bump(userKey(username), now);
        cleanupIfNeeded(now);
    }

    /** Login OK: limpia el contador de la IP y del usuario. */
    public void recordSuccess(String ip, String username) {
        attempts.remove(ipKey(ip));
        attempts.remove(userKey(username));
    }

    private void checkKey(String key, Instant now) {
        Attempt a = attempts.get(key);
        if (a == null) return;
        if (a.lockedUntil != null && now.isBefore(a.lockedUntil)) {
            long secs = Duration.between(now, a.lockedUntil).getSeconds() + 1;
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Probá de nuevo en "
                            + humanize(secs) + ".", secs);
        }
    }

    private void bump(String key, Instant now) {
        attempts.compute(key, (k, a) -> {
            Duration window = Duration.ofMinutes(cfg.getWindowMinutes());
            if (a == null || now.isAfter(a.windowStart.plus(window))) {
                a = new Attempt(now);
            }
            a.count++;
            if (a.count >= cfg.getMaxAttempts()) {
                a.lockedUntil = now.plus(Duration.ofMinutes(cfg.getLockMinutes()));
            }
            return a;
        });
    }

    /** Evita que el mapa crezca sin techo si alguien rota IPs. */
    private void cleanupIfNeeded(Instant now) {
        if (attempts.size() < 1000) return;
        attempts.values().removeIf(a ->
                (a.lockedUntil == null || now.isAfter(a.lockedUntil))
                        && now.isAfter(a.windowStart.plus(Duration.ofMinutes(cfg.getWindowMinutes()))));
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null || ip.isBlank() ? "?" : ip);
    }

    private static String userKey(String username) {
        return "user:" + (username == null ? "" : username.trim().toLowerCase());
    }

    private static String humanize(long seconds) {
        if (seconds < 60) return seconds + " segundos";
        long min = (seconds + 59) / 60;
        return min == 1 ? "1 minuto" : min + " minutos";
    }

    private static final class Attempt {
        int count;
        Instant windowStart;
        Instant lockedUntil;

        Attempt(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
