package com.estilospequenos.common;

/** Regla de negocio violada (ej: eliminar una parametría de sistema). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
