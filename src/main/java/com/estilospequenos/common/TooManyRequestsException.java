package com.estilospequenos.common;

/**
 * Demasiados intentos en poco tiempo (ej: fuerza bruta en el login).
 * Se traduce a HTTP 429 con el header {@code Retry-After}.
 */
public class TooManyRequestsException extends RuntimeException {

    /** Segundos que el cliente tiene que esperar antes de reintentar. */
    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
