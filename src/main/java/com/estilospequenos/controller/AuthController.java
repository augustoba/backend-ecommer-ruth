package com.estilospequenos.controller;

import com.estilospequenos.config.JwtService;
import com.estilospequenos.dto.AccountDtos.RecoverRequest;
import com.estilospequenos.dto.AdminUserDtos.MeResponse;
import com.estilospequenos.dto.LoginRequest;
import com.estilospequenos.dto.TokenResponse;
import com.estilospequenos.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Login del panel de administración. Devuelve el JWT a mandar como Bearer. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        JwtService.TokenData data = authService.login(req.dni(), req.password(), clientIp(http));
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }

    /**
     * Recuperar la cuenta con la frase de recuperación. Setea la contraseña
     * nueva y devuelve un JWT (queda logueado).
     */
    @PostMapping("/recover")
    public ResponseEntity<TokenResponse> recover(@Valid @RequestBody RecoverRequest req, HttpServletRequest http) {
        JwtService.TokenData data =
                authService.recover(req.dni(), req.recoveryPhrase(), req.newPassword(), clientIp(http));
        return ResponseEntity.ok(TokenResponse.bearer(data.token(), data.expiresAt()));
    }

    /** Quién soy y qué permisos tengo (para que el panel muestre/oculte cosas). */
    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        return MeResponse.from(authService.get(auth.getName()));
    }

    /** IP del cliente, respetando el primer hop de {@code X-Forwarded-For} si viene por proxy. */
    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
